package saigonwithlove.ivy.intellij.toolwindow;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import javax.swing.Icon;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;

public class StartEngineAction extends AnAction {
  private IvyEngineService ivyEngineService;

  public StartEngineAction(
      @NotNull String text,
      @NotNull String description,
      @NotNull Icon icon,
      @NotNull IvyEngineService ivyEngineService) {
    super(text, description, icon);
    this.ivyEngineService = ivyEngineService;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    ivyEngineService.startIvyEngine();
  }
}
