package saigonwithlove.ivy.intellij.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.toolwindow.engine.EngineView;
import saigonwithlove.ivy.intellij.toolwindow.module.ModuleView;

public class IvyToolWindowFactory implements ToolWindowFactory {

  @Override
  public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
    EngineView engineView = new EngineView(project);
    Content engineViewContent =
        ContentFactory.getInstance()
            .createContent(engineView, IvyBundle.message("toolWindow.engine.title"), false);
    toolWindow.getContentManager().addContent(engineViewContent);

    ModuleView moduleView = new ModuleView(project);
    Content moduleViewContent =
        ContentFactory.getInstance()
            .createContent(moduleView, IvyBundle.message("toolWindow.module.title"), false);
    toolWindow.getContentManager().addContent(moduleViewContent);
  }

  @Override
  public boolean isApplicable(@NotNull Project project) {
    return project.getService(PreferenceService.class).getState().isPluginEnabled();
  }
}
