package saigonwithlove.ivy.intellij.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.devtool.IvyDevtoolService;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.IvyModule;
import saigonwithlove.ivy.intellij.shared.Notifier;

public class DeployModuleAction extends AnAction {
  private Project project;
  private PreferenceService preferenceService;
  private IvyDevtoolService ivyDevtoolService;
  private JBList<IvyModule> modules;

  public DeployModuleAction(@NotNull Project project, @NotNull JBList<IvyModule> modules) {
    super(
        IvyBundle.message("toolWindow.actions.deployModule.tooltip"),
        IvyBundle.message("toolWindow.actions.deployModule.description"),
        AllIcons.Nodes.Deploy);
    this.project = project;
    this.preferenceService = ServiceManager.getService(project, PreferenceService.class);
    this.ivyDevtoolService = ServiceManager.getService(project, IvyDevtoolService.class);
    this.modules = modules;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    // Show notification if ivy devtool is disabled
    if (!preferenceService.getState().isDevtoolEnabled()) {
      Notifier.info(
          project,
          new OpenUrlAction(
              IvyBundle.message("notification.ivyDevtoolUrlText"),
              "https://github.com/saigonwithlove/ivy-devtool/releases"),
          IvyBundle.message("notification.ivyDevtoolDisabled"));
      return;
    }

    IvyModule selectedIvyModule = modules.getSelectedValue();
    if (Objects.isNull(selectedIvyModule)) {
      Notifier.info(project, IvyBundle.message("notification.deployModuleNotSelected"));
    }

    // Reload module
    Task reloadModuleTask = newReloadModuleTask(project, selectedIvyModule);
    // Sync code with Ivy Engine
    Task syncModuleTask =
        newDeployModuleTask(selectedIvyModule, reloadModuleTask, project, ivyDevtoolService);
    // Compile Java code
    CompileStatusNotification notification =
        (aborted, errors, warnings, compileContext) -> {
          ProgressManager.getInstance().run(syncModuleTask);
        };
    ivyDevtoolService.compileModule(selectedIvyModule, notification);
  }

  private Task newReloadModuleTask(@NotNull Project project, @NotNull IvyModule ivyModule) {
    return new Task.Backgroundable(
        project, IvyBundle.message("tasks.reloadModule.title", ivyModule.getName())) {
      @Override
      public void run(@NotNull ProgressIndicator progressIndicator) {
        progressIndicator.setFraction(0.1);
        progressIndicator.setText(
            IvyBundle.message("tasks.reloadModule.progress.reloading", ivyModule.getName()));
        ivyDevtoolService.reloadModule(ivyModule);
        progressIndicator.setFraction(1);
      }
    };
  }

  @NotNull
  private Task newDeployModuleTask(
      @NotNull IvyModule ivyModule,
      @NotNull Task nextTask,
      @NotNull Project project,
      @NotNull IvyDevtoolService ivyDevtoolService) {
    return new Task.Backgroundable(
        project, IvyBundle.message("tasks.syncModule.title", ivyModule.getName())) {
      @Override
      public void run(@NotNull ProgressIndicator progressIndicator) {
        progressIndicator.setFraction(0.1);
        progressIndicator.setText(
            IvyBundle.message("tasks.reloadModule.progress.connecting", ivyModule.getName()));
        ivyDevtoolService.deployModule(ivyModule, nextTask);
        progressIndicator.setFraction(1);
      }
    };
  }
}
