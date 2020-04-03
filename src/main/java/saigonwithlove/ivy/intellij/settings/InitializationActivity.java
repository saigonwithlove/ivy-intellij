package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import java.util.Collection;
import java.util.Map;
import java.util.Observer;
import java.util.stream.Collectors;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.action.OpenSettingsAction;
import saigonwithlove.ivy.intellij.devtool.IvyDevtoolService;
import saigonwithlove.ivy.intellij.engine.IvyEngineDefinition;
import saigonwithlove.ivy.intellij.engine.IvyEngineRuntime;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.engine.IvyEngineVersions;
import saigonwithlove.ivy.intellij.shared.Configuration;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.IvyModule;
import saigonwithlove.ivy.intellij.shared.Notifier;
import saigonwithlove.ivy.intellij.shared.Projects;

public class InitializationActivity implements StartupActivity {
  @Override
  public void runActivity(@NotNull Project project) {
    IvyDevtoolService ivyDevtoolService =
        ServiceManager.getService(project, IvyDevtoolService.class);
    PreferenceService preferenceService =
        ServiceManager.getService(project, PreferenceService.class);
    PreferenceService.State state = preferenceService.getState();
    PreferenceService.Cache cache = preferenceService.getCache();

    // Update toggle ivy dev tool status
    cache.getIvyEngineDirectoryObservable().addObserver(toggleIvyDevtool(ivyDevtoolService, cache));
    // Update ivy engine definition and libraries
    cache
        .getIvyEngineDirectoryObservable()
        .addObserver(updateIvyEngineDefinitionAndLibraries(project, cache));
    // Restore ivy engine directory from state
    cache.setIvyEngineDirectory(state.getIvyEngineDirectory());
    // Update state for ivy engine directory
    cache.getIvyEngineDirectoryObservable().addObserver(updateIvyEngineDirectoryState(state));

    // Add Ivy Modules cache oberver
    cache
        .getIvyModulesObservable()
        .addObserver((observable, object) -> cache.setEnabled(!cache.getIvyModules().isEmpty()));
    // Initialize Ivy Modules cache
    cache.setIvyModules(Projects.getIvyModules(project));
    // Initialize default global variable cache
    Map<String, String> defaultGlobalVariables =
        cache.getIvyModules().stream()
            .map(IvyModule::getGlobalVariables)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Configuration::getName, Configuration::getValue));
    cache.getIvyEngine().setDefaultGlobalVariables(defaultGlobalVariables);
    // Restore modified global variable from state
    cache.getIvyEngine().setModifiedGlobalVariables(state.getModifiedGlobalVariables());
    // Add modified global variable observer
    cache
        .getIvyEngine()
        .getModifiedGlobalVariablesObservable()
        .addObserver(updateModifiedGlobalVariablesState(state));
  }

  @NotNull
  private Observer updateModifiedGlobalVariablesState(PreferenceService.State state) {
    return (observable, object) -> state.setModifiedGlobalVariables((Map<String, String>) object);
  }

  @NotNull
  private Observer updateIvyEngineDefinitionAndLibraries(
      @NotNull Project project, PreferenceService.Cache cache) {
    return (observable, object) -> {
      IvyEngineService ivyEngineService =
          ServiceManager.getService(project, IvyEngineService.class);
      if (!ivyEngineService.isValidIvyEngine()) {
        Notifier.info(
            project,
            new OpenSettingsAction(project),
            IvyBundle.message("notification.ivyEngineDirectoryInvalid"));
        return;
      }
      ArtifactVersion ivyEngineVersion =
          IvyEngineVersions.parseVersion(cache.getIvyEngineDirectory());
      cache.setIvyEngineDefinition(IvyEngineDefinition.fromVersion(ivyEngineVersion));
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
          IvyEngineRuntime runtime = ivyEngineService.getRuntime();
          runtime
              .getObservable()
              .addObserver(
                  (runtimeObservable, runtimeObject) -> {
                    IvyEngineRuntime rt = (IvyEngineRuntime) runtimeObject;
                    if (rt.getStatus() == IvyEngineRuntime.Status.RUNNING) {
                      rt.stop();
                    }
                  });
          runtime.start();
        }
      }
    };
  }

  @NotNull
  private Observer toggleIvyDevtool(
      IvyDevtoolService ivyDevtoolService, PreferenceService.Cache cache) {
    return (observable, object) -> cache.getIvyDevtool().setEnabled(ivyDevtoolService.exists());
  }

  @NotNull
  private Observer updateIvyEngineDirectoryState(PreferenceService.State state) {
    return (observable, object) -> state.setIvyEngineDirectory((String) object);
  }
}
