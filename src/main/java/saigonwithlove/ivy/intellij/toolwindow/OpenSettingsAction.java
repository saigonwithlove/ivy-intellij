package saigonwithlove.ivy.intellij.toolwindow;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

public class OpenSettingsAction extends AnAction {
  private Project project;

  public OpenSettingsAction(
      @NotNull String text,
      @NotNull String description,
      @NotNull Icon icon,
      @NotNull Project project) {
    super(text, description, icon);
    this.project = project;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    ShowSettingsUtil.getInstance()
        .showSettingsDialog(project, IvyBundle.message("settings.displayName"));
  }
}
