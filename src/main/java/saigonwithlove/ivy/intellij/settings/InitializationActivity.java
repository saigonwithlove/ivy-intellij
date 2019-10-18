package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;

public class InitializationActivity implements StartupActivity {
  @Override
  public void runActivity(@NotNull Project project) {
    PreferenceService.State preferences =
        ServiceManager.getService(project, PreferenceService.class).getState();
    updateLibraries(project, preferences);
    toggleIvyDevtoolSetting(project, preferences);
  }

  private void toggleIvyDevtoolSetting(
      @NotNull Project project, PreferenceService.State preferences) {
    preferences.setIvyDevToolEnabled(
        ModuleManager.getInstance(project).findModuleByName("ivy-devtool") != null);
  }

  private void updateLibraries(@NotNull Project project, PreferenceService.State preferences) {
    if (StringUtils.isNotBlank(preferences.getIvyEngineDirectory())) {
      ApplicationManager.getApplication()
          .runWriteAction(
              () ->
                  ServiceManager.getService(project, IvyEngineService.class)
                      .addLibraries(preferences.getIvyEngineDirectory()));
    }
  }
}
