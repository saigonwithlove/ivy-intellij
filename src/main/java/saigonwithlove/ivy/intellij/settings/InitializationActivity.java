package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.startup.StartupActivity;
import io.reactivex.rxjava3.core.Observer;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.action.OpenSettingsAction;
import saigonwithlove.ivy.intellij.engine.Ivy6Library;
import saigonwithlove.ivy.intellij.engine.Ivy7Library;
import saigonwithlove.ivy.intellij.engine.Ivy8Library;
import saigonwithlove.ivy.intellij.engine.IvyEngine;
import saigonwithlove.ivy.intellij.engine.IvyEngineFactory;
import saigonwithlove.ivy.intellij.engine.IvyLibraries;
import saigonwithlove.ivy.intellij.engine.IvyLibrary;
import saigonwithlove.ivy.intellij.shared.Configuration;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.IvyModule;
import saigonwithlove.ivy.intellij.shared.Notifier;
import saigonwithlove.ivy.intellij.shared.Projects;

public class InitializationActivity implements StartupActivity {
  private static final Logger LOG =
      Logger.getInstance("#" + InitializationActivity.class.getCanonicalName());

  @Override
  public void runActivity(@NotNull Project project) {
    PreferenceService preferenceService = project.getService(PreferenceService.class);

    LOG.info("subscribe to create Ivy Engine according to changed Ivy Engine Directory.");
    preferenceService
        .asObservable()
        .map(PreferenceService.State::getIvyEngineDirectory)
        .subscribe(createIvyEngineUpdater(preferenceService, project));

    LOG.info(
        "subscribe to update IntelliJ Libraries when new Ivy Engine created"
            + "or delete Ivy related libraries when no Ivy Engine.");
    preferenceService
        .asObservable()
        .map(state -> Optional.ofNullable(state.getIvyEngine()))
        .subscribe(createIntellijLibrariesUpdater());

    /*
     * TODO should subscribe to module change and update Ivy Modules.
     */
    Runnable miscUpdater =
        () -> {
          preferenceService.update(
              state -> {
                List<IvyModule> ivyModules = Projects.getIvyModules(project);
                LOG.info("Update Ivy Modules into State: " + ivyModules);
                state.setIvyModules(ivyModules);
                LOG.info("Disable Ivy Plugin if no Ivy Module existed in project.");
                state.setPluginEnabled(!ivyModules.isEmpty());

                LOG.info("Synchronize Global Variables.");
                Map<String, Configuration> globalVariables =
                    ivyModules.stream()
                        .map(IvyModule::getGlobalVariables)
                        .flatMap(Collection::stream)
                        .collect(
                            Collectors.toMap(
                                Configuration::getName, configuration -> configuration));
                Map<String, Configuration> storedGlobalVariables = state.getGlobalVariables();
                storedGlobalVariables.forEach(
                    (storedName, storedConfiguration) -> {
                      globalVariables.computeIfPresent(
                          storedName,
                          (name, configuration) -> {
                            if (storedConfiguration.isModified()) {
                              configuration.setValue(storedConfiguration.getValue());
                              LOG.info(
                                  MessageFormat.format(
                                      "Synchronized variable: {0} with stored value: {1}",
                                      name, configuration.getValue()));
                            }
                            return configuration;
                          });
                    });
                state.setGlobalVariables(globalVariables);
                return state;
              });
        };
    ApplicationManager.getApplication().invokeLater(miscUpdater);
  }

  private Observer<Optional<IvyEngine>> createIntellijLibrariesUpdater() {
    return new CacheObserver<>(
        "Update Intellij Libraries",
        ivyEngineOpt -> {
          Runnable runner =
              () -> {
                if (ivyEngineOpt.isPresent()) {
                  ivyEngineOpt.get().buildIntellijLibraries();
                } else {
                  LibraryTable libraryTable =
                      LibraryTablesRegistrar.getInstance().getLibraryTable();
                  LibraryTable.ModifiableModel modifiableModel = libraryTable.getModifiableModel();
                  Stream.of(Ivy6Library.values(), Ivy7Library.values(), Ivy8Library.values())
                      .flatMap((Function<IvyLibrary[], Stream<IvyLibrary>>) Arrays::stream)
                      .map(IvyLibrary::getName)
                      .forEach(
                          libraryName -> IvyLibraries.removeLibrary(libraryTable, libraryName));
                  modifiableModel.commit();
                }
              };
          ApplicationManager.getApplication().runWriteAction(runner);
        });
  }

  @NotNull
  private Observer<String> createIvyEngineUpdater(
      @NotNull PreferenceService preferenceService, @NotNull Project project) {
    return new CacheObserver<>(
        "Create Ivy Engine from ivyEngineDirectory",
        ivyEngineDirectory -> {
          try {
            IvyEngineFactory ivyEngineFactory = new IvyEngineFactory(ivyEngineDirectory, project);
            IvyEngine engine = ivyEngineFactory.newEngine();
            LOG.info(
                "Initialize Ivy Engine with Devtool, and other configurations to work with Ivy Plugin.");
            engine.initialize();
            preferenceService.update(
                state -> {
                  state.setIvyEngine(engine);
                  return state;
                });
            LOG.info("Create Ivy Engine: " + engine.getVersion());
          } catch (NoSuchElementException ex) {
            preferenceService.update(
                state -> {
                  state.setIvyEngine(null);
                  return state;
                });
            LOG.error("Could not create Ivy Engine.", ex);
            Notifier.info(
                project,
                new OpenSettingsAction(project),
                IvyBundle.message("notification.ivyEngineDirectoryInvalid"));
          }
        });
  }
}
