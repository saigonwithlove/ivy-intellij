package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.action.OpenSettingsAction;
import saigonwithlove.ivy.intellij.engine.IvyEngineDefinition;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.engine.IvyEngineVersions;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.Modules;
import saigonwithlove.ivy.intellij.shared.Projects;
import saigonwithlove.ivy.intellij.shared.Notifier;

public class InitializationActivity implements StartupActivity {
  @Override
  public void runActivity(@NotNull Project project) {
    PreferenceService.State preferences =
        ServiceManager.getService(project, PreferenceService.class).getState();

    if (Projects.getIvyModels(project).isEmpty()) {
      return;
    } else {
      preferences.setEnabled(true);
    }

    IvyEngineService ivyEngineService = ServiceManager.getService(project, IvyEngineService.class);
    if (!ivyEngineService.isValidIvyEngine()) {
      Notifier.info(
          project,
          new OpenSettingsAction(project),
          IvyBundle.message("notification.ivyEngineDirectoryInvalid"));
      return;
    }

    ArtifactVersion ivyEngineVersion =
        IvyEngineVersions.parseVersion(preferences.getIvyEngineDirectory());
    preferences.setIvyEngineDefinition(IvyEngineDefinition.fromVersion(ivyEngineVersion));
    if (ivyEngineService.libraryDirectoryExists()) {
      ApplicationManager.getApplication()
          .runWriteAction(
              () -> ServiceManager.getService(project, IvyEngineService.class).addLibraries());
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
        ivyEngineService.startIvyEngine();
      }
    }
    toggleIvyDevtoolSetting(project, preferences);
  }

  private void toggleIvyDevtoolSetting(
      @NotNull Project project, @NotNull PreferenceService.State preferences) {
    preferences.setIvyDevToolEnabled(
        ModuleManager.getInstance(project).findModuleByName(Modules.IVY_DEVTOOL) != null);
  }
}
