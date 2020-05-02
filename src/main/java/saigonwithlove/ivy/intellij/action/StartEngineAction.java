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
import java.util.Observer;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.devtool.IvyDevtoolService;
import saigonwithlove.ivy.intellij.engine.IvyEngineRuntime;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.IvyModule;
import saigonwithlove.ivy.intellij.shared.Notifier;

public class StartEngineAction extends AnAction {
  private static final Logger LOG =
      Logger.getInstance("#" + StartEngineAction.class.getCanonicalName());

  private Project project;
  private PreferenceService preferenceService;
  private IvyEngineService ivyEngineService;
  private IvyDevtoolService ivyDevtoolService;

  public StartEngineAction(@NotNull Project project) {
    super(
        IvyBundle.message("toolWindow.actions.startEngine.tooltip"),
        IvyBundle.message("toolWindow.actions.startEngine.description"),
        AllIcons.Actions.Execute);
    this.project = project;
    this.preferenceService = ServiceManager.getService(project, PreferenceService.class);
    this.ivyEngineService = ServiceManager.getService(project, IvyEngineService.class);
    this.ivyDevtoolService = ServiceManager.getService(project, IvyDevtoolService.class);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    if (!ivyEngineService.isValidIvyEngine()) {
      Notifier.info(
          project,
          new OpenSettingsAction(project),
          IvyBundle.message("notification.ivyEngineDirectoryInvalid"));
      return;
    }

    if (ivyDevtoolService.notExists() || ivyDevtoolService.isOutdated()) {
      Boolean ivyDevtoolInstalled = ProgressManager.getInstance().run(newInstallIvyDevtoolTask());
      preferenceService.getCache().getIvyDevtool().setEnabled(ivyDevtoolInstalled);
    }
    if (preferenceService.getCache().getIvyDevtool().isEnabled()) {
      ProgressManager.getInstance().run(newDeployIvyModulesTask());
      Observer runtimeReadyObserver =
          (observable, object) -> {
            IvyEngineRuntime rt = (IvyEngineRuntime) object;
            if (rt.getStatus() == IvyEngineRuntime.Status.RUNNING) {
              ProgressManager.getInstance().run(newUpdateGlobalVariablesTask());
              ProgressManager.getInstance().run(newUpdateServerPropertiesTask());
            }
          };
      IvyEngineRuntime runtime = ivyEngineService.getRuntime();
      runtime.getObservable().addObserver(runtimeReadyObserver);
      runtime.start();
    }
  }

  @NotNull
  private Task.Backgroundable newUpdateGlobalVariablesTask() {
    return new Task.Backgroundable(
        project,
        IvyBundle.message("toolWindow.actions.startEngine.progress.updateGlobalVariables")) {
      @Override
      public void run(@NotNull ProgressIndicator indicator) {
        Map<String, String> globalVariables =
            preferenceService.getCache().getIvyEngine().getGlobalVariables();
        Map<String, String> modifiedGlobalVariables =
            preferenceService.getCache().getIvyEngine().getModifiedGlobalVariables();
        modifiedGlobalVariables.entrySet().stream()
            .filter(entry -> globalVariables.containsKey(entry.getKey()))
            .forEach(
                entry -> {
                  indicator.setFraction(1.0 - indicator.getFraction() / 2);
                  ivyDevtoolService.updateGlobalVariable(entry.getKey(), entry.getValue());
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
        Map<String, String> serverProperties = ivyDevtoolService.getServerProperties();
        Map<String, String> modifiedServerProperties =
            preferenceService.getCache().getIvyEngine().getModifiedServerProperties();
        modifiedServerProperties.entrySet().stream()
            .filter(entry -> serverProperties.containsKey(entry.getKey()))
            .forEach(
                entry -> ivyDevtoolService.updateServerProperty(entry.getKey(), entry.getValue()));
        preferenceService.update(
            cache -> cache.getIvyEngine().setServerProperties(serverProperties));
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
            preferenceService.getCache().getIvyModules().stream()
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
