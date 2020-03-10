package saigonwithlove.ivy.intellij.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

public class StartEngineAction extends AnAction {
  private IvyEngineService ivyEngineService;

  public StartEngineAction(@NotNull IvyEngineService ivyEngineService) {
    super(
        IvyBundle.message("toolWindow.actions.startEngine.tooltip"),
        IvyBundle.message("toolWindow.actions.startEngine.description"),
        AllIcons.Actions.Execute);
    this.ivyEngineService = ivyEngineService;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    ivyEngineService.startIvyEngine();
  }
}
