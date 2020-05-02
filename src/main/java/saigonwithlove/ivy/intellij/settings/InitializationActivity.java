package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import java.util.Collection;
import java.util.List;
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
    PreferenceService preferenceService =
        ServiceManager.getService(project, PreferenceService.class);
    PreferenceService.State state = preferenceService.getState();

    // Update ivy engine definition and libraries
    preferenceService.addObserver(updateIvyEngineDefinition(project));
    preferenceService.addObserver(updateIvyDevtoolStatus(project));
    preferenceService.addObserver(updateIvyEngineLibraries(project));
    // Restore ivy engine directory from state
    preferenceService.update(cache -> cache.setIvyEngineDirectory(state.getIvyEngineDirectory()));
    // Update state for ivy engine directory
    preferenceService.addObserver(updateIvyEngineDirectoryState(state));

    // Add Ivy Modules cache oberver
    preferenceService.addObserver(updateIvyPluginStatus(project));
    // Initialize Ivy Modules cache
    preferenceService.update(cache -> cache.setIvyModules(Projects.getIvyModules(project)));
    // Initialize default global variable cache
    Map<String, String> defaultGlobalVariables =
        preferenceService.getCache().getIvyModules().stream()
            .map(IvyModule::getGlobalVariables)
            .flatMap(Collection::stream)
            .collect(Collectors.toMap(Configuration::getName, Configuration::getValue));
    preferenceService.update(
        cache -> cache.getIvyEngine().setGlobalVariables(defaultGlobalVariables));
    // Restore modified global variable from state
    preferenceService.update(
        cache ->
            cache.getIvyEngine().setModifiedGlobalVariables(state.getModifiedGlobalVariables()));
    // Add modified global variable observer
    preferenceService.addObserver(updateModifiedGlobalVariablesState(state));
    // Add modified server property observer
    preferenceService.addObserver(updateModifiedServerPropertiesState(state));
  }

  private Observer updateModifiedServerPropertiesState(PreferenceService.State state) {
    CacheObserver.Converter<Map<String, String>> converter =
        cache -> cache.getIvyEngine().getModifiedServerProperties();
    return new CacheObserver<>(
        "Update Modified Server Properties in State",
        converter,
        state::setModifiedServerProperties);
  }

  private Observer updateIvyPluginStatus(@NotNull Project project) {
    CacheObserver.Updater<List<IvyModule>> updater =
        ivyModules -> {
          PreferenceService preferenceService =
              ServiceManager.getService(project, PreferenceService.class);
          preferenceService.update(cache -> cache.setEnabled(!cache.getIvyModules().isEmpty()));
        };
    return new CacheObserver<>(
        "Update Ivy Plugin status", PreferenceService.Cache::getIvyModules, updater);
  }

  @NotNull
  private Observer updateModifiedGlobalVariablesState(PreferenceService.State state) {
    CacheObserver.Converter<Map<String, String>> converter =
        cache -> cache.getIvyEngine().getModifiedGlobalVariables();
    return new CacheObserver<>(
        "Update Modified Global Variables in State", converter, state::setModifiedGlobalVariables);
  }

  @NotNull
  Observer updateIvyEngineDefinition(@NotNull Project project) {
    CacheObserver.Converter<String> converter =
        o -> ((PreferenceService.Cache) o).getIvyEngineDirectory();
    CacheObserver.Updater<String> updater =
        ivyEngineDirectory -> {
          IvyEngineService ivyEngineService =
              ServiceManager.getService(project, IvyEngineService.class);
          PreferenceService preferenceService =
              ServiceManager.getService(project, PreferenceService.class);
          if (!ivyEngineService.isValidIvyEngine()) {
            Notifier.info(
                project,
                new OpenSettingsAction(project),
                IvyBundle.message("notification.ivyEngineDirectoryInvalid"));
            return;
          }
          ArtifactVersion ivyEngineVersion = IvyEngineVersions.parseVersion(ivyEngineDirectory);
          preferenceService.update(
              cache ->
                  cache.setIvyEngineDefinition(IvyEngineDefinition.fromVersion(ivyEngineVersion)));
        };
    return new CacheObserver<>("Update Ivy Engine Definition", converter, updater);
  }

  @NotNull
  private Observer updateIvyDevtoolStatus(@NotNull Project project) {
    CacheObserver.Updater<IvyEngineDefinition> updater =
        ivyEngineDefinition -> {
          PreferenceService preferenceService =
              ServiceManager.getService(project, PreferenceService.class);
          if (ivyEngineDefinition != null) {
            IvyDevtoolService ivyDevtoolService =
                ServiceManager.getService(project, IvyDevtoolService.class);
            // Update toggle ivy dev tool status
            preferenceService.update(
                cache -> cache.getIvyDevtool().setEnabled(ivyDevtoolService.exists()));
          } else {
            preferenceService.update(cache -> cache.getIvyDevtool().setEnabled(false));
          }
        };
    return new CacheObserver<>(
        "Update Ivy Devtool Status", PreferenceService.Cache::getIvyEngineDefinition, updater);
  }

  @NotNull
  private Observer updateIvyEngineLibraries(@NotNull Project project) {
    CacheObserver.Updater<IvyEngineDefinition> updater =
        ivyEngineDefinition -> {
          IvyEngineService ivyEngineService =
              ServiceManager.getService(project, IvyEngineService.class);

          if (ivyEngineService.libraryDirectoryExists()) {
            ApplicationManager.getApplication()
                .runWriteAction(
                    () ->
                        ServiceManager.getService(project, IvyEngineService.class).addLibraries());
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
    return new CacheObserver<>(
        "Update Ivy Engine Libraries", PreferenceService.Cache::getIvyEngineDefinition, updater);
  }

  @NotNull
  private Observer updateIvyEngineDirectoryState(PreferenceService.State state) {
    return new CacheObserver<>(
        "Update Ivy Engine Directory in State",
        PreferenceService.Cache::getIvyEngineDirectory,
        state::setIvyEngineDirectory);
  }
}
