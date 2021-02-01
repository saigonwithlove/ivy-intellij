package saigonwithlove.ivy.intellij.toolwindow;

import com.intellij.openapi.components.ServiceManager;
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
        ContentFactory.SERVICE
            .getInstance()
            .createContent(engineView, IvyBundle.message("toolWindow.engine.title"), false);
    toolWindow.getContentManager().addContent(engineViewContent);

    ModuleView moduleView = new ModuleView(project);
    Content moduleViewContent =
        ContentFactory.SERVICE
            .getInstance()
            .createContent(moduleView, IvyBundle.message("toolWindow.module.title"), false);
    toolWindow.getContentManager().addContent(moduleViewContent);
  }

  @Override
  public boolean isApplicable(@NotNull Project project) {
    return ServiceManager.getService(project, PreferenceService.class).getState().isPluginEnabled();
  }
}
