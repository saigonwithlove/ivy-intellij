package saigonwithlove.ivy.intellij.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngine;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.IvyModule;
import saigonwithlove.ivy.intellij.shared.Modules;
import saigonwithlove.ivy.intellij.shared.Notifier;

public class DeployModuleAction extends AnAction {
  private Project project;
  private PreferenceService preferenceService;
  private JBList<IvyModule> modules;

  public DeployModuleAction(@NotNull Project project, @NotNull JBList<IvyModule> modules) {
    super(
        IvyBundle.message("toolWindow.actions.deployModule.tooltip"),
        IvyBundle.message("toolWindow.actions.deployModule.description"),
        AllIcons.Nodes.Deploy);
    this.project = project;
    this.preferenceService = project.getService(PreferenceService.class);
    this.modules = modules;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    IvyModule selectedIvyModule = modules.getSelectedValue();
    if (Objects.isNull(selectedIvyModule)) {
      Notifier.info(project, IvyBundle.message("notification.deployModuleNotSelected"));
      return;
    }

    IvyEngine ivyEngine = preferenceService.getState().getIvyEngine();
    if (Objects.isNull(ivyEngine)) {
      // TODO should notify that Ivy Engine is null.
      return;
    }

    // Deploy to Ivy Engine
    Task syncModuleTask = newDeployModuleTask(project, ivyEngine, selectedIvyModule);
    // Compile Java code
    Modules.compile(project, selectedIvyModule)
        .subscribe(() -> ProgressManager.getInstance().run(syncModuleTask));
  }

  @NotNull
  private Task newDeployModuleTask(
      @NotNull Project project, @NotNull IvyEngine ivyEngine, @NotNull IvyModule ivyModule) {
    return new Task.Backgroundable(
        project, IvyBundle.message("tasks.syncModule.title", ivyModule.getName())) {
      @Override
      public void run(@NotNull ProgressIndicator progressIndicator) {
        // TODO change the notification to match with current context.
        progressIndicator.setFraction(0.1);
        progressIndicator.setText(
            IvyBundle.message("tasks.reloadModule.progress.connecting", ivyModule.getName()));
        ivyEngine.deployIvyModule(ivyModule);
        progressIndicator.setFraction(1);
      }
    };
  }
}
