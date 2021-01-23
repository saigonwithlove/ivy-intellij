package saigonwithlove.ivy.intellij.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.devtool.IvyDevtoolService;
import saigonwithlove.ivy.intellij.engine.IvyEngine;
import saigonwithlove.ivy.intellij.settings.CacheObserver;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.Configuration;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.IvyModule;
import saigonwithlove.ivy.intellij.shared.Notifier;

public class StartEngineAction extends AnAction {
  private static final Logger LOG =
      Logger.getInstance("#" + StartEngineAction.class.getCanonicalName());

  private Project project;
  private PreferenceService preferenceService;
  private IvyDevtoolService ivyDevtoolService;

  public StartEngineAction(@NotNull Project project) {
    super(
        IvyBundle.message("toolWindow.actions.startEngine.tooltip"),
        IvyBundle.message("toolWindow.actions.startEngine.description"),
        AllIcons.Actions.Execute);
    this.project = project;
    this.preferenceService = ServiceManager.getService(project, PreferenceService.class);
    this.ivyDevtoolService = ServiceManager.getService(project, IvyDevtoolService.class);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    IvyEngine engine = preferenceService.getState().getIvyEngine();
    if (engine == null) {
      Notifier.info(
          project,
          new OpenSettingsAction(project),
          IvyBundle.message("notification.ivyEngineDirectoryInvalid"));
      return;
    }

    if (ivyDevtoolService.notExists() || ivyDevtoolService.isOutdated()) {
      Boolean ivyDevtoolInstalled = ProgressManager.getInstance().run(newInstallIvyDevtoolTask());
      preferenceService.update(
          state -> {
            state.setDevtoolEnabled(ivyDevtoolInstalled);
            return state;
          });
    }
    if (preferenceService.getState().isDevtoolEnabled()) {
      ProgressManager.getInstance().run(newDeployIvyModulesTask());
      CacheObserver<IvyEngine.Status> runtimeReadyObserver =
          new CacheObserver<>(
              "Update Global Variables and Server Properties.",
              ivyEngineStatus -> {
                if (ivyEngineStatus == IvyEngine.Status.RUNNING) {
                  ProgressManager.getInstance().run(newUpdateGlobalVariablesTask());
                  ProgressManager.getInstance().run(newUpdateServerPropertiesTask());
                }
              });
      preferenceService
          .asObservable()
          .map(PreferenceService.State::getIvyEngine)
          .filter(Objects::nonNull)
          .map(IvyEngine::getStatus)
          .subscribe(runtimeReadyObserver);
      engine.start();
    }
  }

  @NotNull
  private Task.Backgroundable newUpdateGlobalVariablesTask() {
    return new Task.Backgroundable(
        project,
        IvyBundle.message("toolWindow.actions.startEngine.progress.updateGlobalVariables")) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        Map<String, Configuration> globalVariables =
            preferenceService.getState().getGlobalVariables();
        globalVariables.entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(Configuration::isModified)
            .forEach(
                globalVariable -> {
                  indicator.setFraction(1.0 - indicator.getFraction() / 2);
                  ivyDevtoolService.updateGlobalVariable(
                      globalVariable.getName(), globalVariable.getValue());
                });
      }
    };
  }

  @NotNull
  private Task.Backgroundable newUpdateServerPropertiesTask() {
    return new Task.Backgroundable(
        project,
        IvyBundle.message("toolWindow.actions.startEngine.progress.updateServerProperties")) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        Map<String, Configuration> storedServerProperties =
            preferenceService.getState().getServerProperties();
        Map<String, Configuration> serverProperties =
            ivyDevtoolService.getServerProperties().entrySet().stream()
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> {
                          Configuration serverProperty =
                              Configuration.builder()
                                  .name(entry.getKey())
                                  .defaultValue(entry.getValue())
                                  .value(
                                      Optional.ofNullable(
                                              storedServerProperties.get(entry.getKey()))
                                          .filter(Configuration::isModified)
                                          .map(Configuration::getValue)
                                          .orElse(null))
                                  .build();
                          return serverProperty;
                        }));
        serverProperties.entrySet().stream()
            .map(Map.Entry::getValue)
            .filter(Configuration::isModified)
            .forEach(
                serverProperty -> {
                  ivyDevtoolService.updateServerProperty(
                      serverProperty.getName(), serverProperty.getValue());
                });
        preferenceService.update(
            state -> {
              state.setServerProperties(serverProperties);
              return state;
            });
      }
    };
  }

  @NotNull
  private Task.WithResult<Void, RuntimeException> newDeployIvyModulesTask() {
    return new Task.WithResult<Void, RuntimeException>(
        project,
        IvyBundle.message("toolWindow.actions.startEngine.progress.deployIvyModules"),
        false) {
      @Override
      protected Void compute(@NotNull ProgressIndicator indicator) throws RuntimeException {
        List<IvyModule> ivyModules =
            preferenceService.getState().getIvyModules().stream()
                .filter(ivyDevtoolService::isNotDeployed)
                .collect(Collectors.toList());
        for (int i = 0; i < ivyModules.size(); i++) {
          ivyDevtoolService.deployModule(ivyModules.get(i));
          indicator.setFraction(((double) i) / (ivyModules.size()));
        }
        return null;
      }
    };
  }

  @NotNull
  private Task.WithResult<Boolean, RuntimeException> newInstallIvyDevtoolTask() {
    return new Task.WithResult<Boolean, RuntimeException>(
        project, IvyBundle.message("tasks.installIvyDevtool.title"), false) {
      @Override
      protected Boolean compute(@NotNull ProgressIndicator indicator) throws RuntimeException {
        try {
          ivyDevtoolService.install(indicator);
          return Boolean.TRUE;
        } catch (RuntimeException ex) {
          LOG.error(ex);
          return Boolean.FALSE;
        }
      }
    };
  }
}
