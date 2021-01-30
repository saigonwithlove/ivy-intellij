package saigonwithlove.ivy.intellij.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.text.MessageFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngine;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.Configuration;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.Notifier;

public class StartEngineAction extends AnAction {
  private static final Logger LOG =
      Logger.getInstance("#" + StartEngineAction.class.getCanonicalName());

  private final Project project;
  private final PreferenceService preferenceService;

  public StartEngineAction(@NotNull Project project) {
    super(
        IvyBundle.message("toolWindow.actions.startEngine.tooltip"),
        IvyBundle.message("toolWindow.actions.startEngine.description"),
        AllIcons.Actions.Execute);
    this.project = project;
    this.preferenceService = ServiceManager.getService(project, PreferenceService.class);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    IvyEngine ivyEngine = preferenceService.getState().getIvyEngine();
    if (ivyEngine == null) {
      Notifier.info(
          project,
          new OpenSettingsAction(project),
          IvyBundle.message("notification.ivyEngineDirectoryInvalid"));
      return;
    }

    if (ivyEngine.getStatus() == IvyEngine.Status.RUNNING) {
      LOG.info(
          MessageFormat.format("Ivy Engine is {0}, skip start process.", ivyEngine.getStatus()));
      return;
    }

    ivyEngine
        .start()
        .timeout(120, TimeUnit.SECONDS)
        .doOnSuccess(
            item -> {
              LOG.info("Deploy all modules.");
              preferenceService.getState().getIvyModules().stream()
                  .filter(item::isIvyModuleNotDeployed)
                  .forEach(item::deployIvyModule);
            })
        .doOnSuccess(
            item -> {
              LOG.info("Update global variables.");
              Map<String, Configuration> globalVariables =
                  preferenceService.getState().getGlobalVariables();
              globalVariables.values().stream()
                  .filter(Configuration::isModified)
                  .forEach(item::updateGlobalVariable);
            })
        .subscribe(
            item -> {
              LOG.info("Update server properties.");
              // TODO Ivy Engine should be then entry point for updating server properties.
              Map<String, Configuration> storedServerProperties =
                  preferenceService.getState().getServerProperties();
              Map<String, Configuration> serverProperties = item.getServerProperties();
              storedServerProperties.forEach(
                  (storedName, storedConfiguration) -> {
                    serverProperties.computeIfPresent(
                        storedName,
                        (name, configuration) -> {
                          if (StringUtils.equals(
                              storedConfiguration.getValue(), configuration.getValue())) {
                            configuration.setValue(storedConfiguration.getValue());
                          }
                          return configuration;
                        });
                  });
              preferenceService.update(
                  state -> {
                    state.setServerProperties(serverProperties);
                    return state;
                  });
              serverProperties.values().stream()
                  .filter(Configuration::isModified)
                  .forEach(item::updateServerProperty);
            },
            ex -> LOG.error("Could not get the Status.RUNNING after starting Ivy Engine.", ex));
  }
}
