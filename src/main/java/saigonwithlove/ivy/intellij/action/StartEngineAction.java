package saigonwithlove.ivy.intellij.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.devtool.IvyDevtoolService;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

public class StartEngineAction extends AnAction {
  private static final Logger LOG =
      Logger.getInstance("#" + StartEngineAction.class.getCanonicalName());

  private Project project;
  private PreferenceService preferenceService;
  private IvyEngineService ivyEngineService;
  private IvyDevtoolService ivyDevtoolService;

  public StartEngineAction(
      @NotNull Project project,
      @NotNull PreferenceService preferenceService,
      @NotNull IvyEngineService ivyEngineService,
      @NotNull IvyDevtoolService ivyDevtoolService) {
    super(
        IvyBundle.message("toolWindow.actions.startEngine.tooltip"),
        IvyBundle.message("toolWindow.actions.startEngine.description"),
        AllIcons.Actions.Execute);
    this.project = project;
    this.preferenceService = preferenceService;
    this.ivyEngineService = ivyEngineService;
    this.ivyDevtoolService = ivyDevtoolService;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    if (ivyDevtoolService.exists()) {
      ivyEngineService.startIvyEngine();
    } else {
      Task.WithResult<Boolean, RuntimeException> installIvyDevtoolTask =
          newInstallIvyDevtoolTask(this.project, this.ivyDevtoolService);
      Boolean ivyDevtoolInstalled = ProgressManager.getInstance().run(installIvyDevtoolTask);
      this.preferenceService.getState().setIvyDevToolEnabled(ivyDevtoolInstalled);
      if (ivyDevtoolInstalled) {
        ivyEngineService.startIvyEngine();
      }
    }
  }

  @NotNull
  private Task.WithResult<Boolean, RuntimeException> newInstallIvyDevtoolTask(
      @NotNull Project project, @NotNull IvyDevtoolService ivyDevtoolService) {
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
