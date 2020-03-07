package saigonwithlove.ivy.intellij.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

public class OpenSettingsAction extends AnAction {
  private Project project;

  public OpenSettingsAction(Project project) {
    super(
        IvyBundle.message("toolWindow.actions.settings.tooltip"),
        IvyBundle.message("toolWindow.actions.settings.description"),
        AllIcons.General.Settings);
    this.project = project;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    ShowSettingsUtil.getInstance()
        .showSettingsDialog(project, IvyBundle.message("settings.displayName"));
  }
}
