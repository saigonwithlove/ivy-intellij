package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.shared.IvyEngine;
import saigonwithlove.ivy.intellij.shared.LibrarySyncInvoker;

public class InitializationActivity implements StartupActivity {
  @Override
  public void runActivity(@NotNull Project project) {
    PreferenceService.State preferences =
        ServiceManager.getService(project, PreferenceService.class).getState();
    ServiceManager.getService(project, LibrarySyncInvoker.class).syncLibraries(project, preferences);
    toggleIvyDevtoolSetting(project, preferences);
  }

  private void toggleIvyDevtoolSetting(
      @NotNull Project project, PreferenceService.State preferences) {
    preferences.setIvyDevToolEnabled(
        ModuleManager.getInstance(project).findModuleByName("ivy-devtool") != null);
  }

}
