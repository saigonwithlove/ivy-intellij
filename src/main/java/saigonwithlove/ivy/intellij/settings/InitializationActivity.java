package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.Modules;

public class InitializationActivity implements StartupActivity {
  @Override
  public void runActivity(@NotNull Project project) {
    PreferenceService.State preferences =
        ServiceManager.getService(project, PreferenceService.class).getState();
    IvyEngineService ivyEngineService = ServiceManager.getService(project, IvyEngineService.class);
    if (ivyEngineService.isOsgiFolderExist(preferences.getIvyEngineDirectory())) {
      ApplicationManager.getApplication()
          .runWriteAction(
              () ->
                  ServiceManager.getService(project, IvyEngineService.class)
                      .addLibraries(preferences.getIvyEngineDirectory()));
    } else {
      int result =
          MessageDialogBuilder.yesNo(
                  IvyBundle.message("settings.engine.startEngineDialog.title"),
                  IvyBundle.message("settings.engine.startEngineDialog.message"))
              .yesText(IvyBundle.message("settings.engine.startEngineDialog.confirm"))
              .noText(IvyBundle.message("settings.engine.startEngineDialog.cancel"))
              .project(project)
              .show();
      if (result == Messages.YES) {
        ivyEngineService.startIvyEngine(preferences.getIvyEngineDirectory(), project);
      }
    }
    toggleIvyDevtoolSetting(project, preferences);
  }

  private void toggleIvyDevtoolSetting(
      @NotNull Project project, PreferenceService.State preferences) {
    preferences.setIvyDevToolEnabled(
        ModuleManager.getInstance(project).findModuleByName(Modules.IVY_DEVTOOL) != null);
  }
}
