package saigonwithlove.ivy.intellij.engine;

import com.google.common.collect.Lists;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import java.io.File;
import java.util.List;
import java.util.Optional;
import org.apache.http.client.fluent.Request;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.GeneralRunProfile;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

public class IvyEngineService {
  private static final Logger LOG =
      Logger.getInstance("#" + IvyEngineService.class.getCanonicalName());
  private Project project;
  private PreferenceService preferenceService;

  public IvyEngineService(Project project) {
    this.project = project;
    this.preferenceService = ServiceManager.getService(project, PreferenceService.class);
  }

  public void startIvyEngine() {
    String ivyEngineDirectory = preferenceService.getState().getIvyEngineDirectory();
    String ivyCommand = preferenceService.getState().getIvyEngineDefinition().getStartCommand();
    GeneralCommandLine commandLine = new GeneralCommandLine(ivyEngineDirectory + ivyCommand);
    commandLine.setWorkDirectory(ivyEngineDirectory);
    commandLine.addParameters("start");
    try {
      ExecutionEnvironment environment =
          ExecutionEnvironmentBuilder.create(
                  project,
                  DefaultRunExecutor.getRunExecutorInstance(),
                  new GeneralRunProfile(commandLine, IvyBundle.message("tasks.runIvyEngine.title")))
              .executionId(ExecutionEnvironment.getNextUnusedExecutionId())
              .build();
      environment.getRunner().execute(environment);
    } catch (ExecutionException ex) {
      LOG.error(ex);
    }
  }

  public void addLibraries() {
    LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable();
    preferenceService
        .getState()
        .getIvyEngineDefinition()
        .getLibraries()
        .forEach(
            ivyLibrary ->
                IvyLibraries.defineLibrary(
                    preferenceService.getState().getIvyEngineDirectory(),
                    libraryTable,
                    ivyLibrary));
  }

  @NotNull
  public Optional<String> getIvyEngineUrl() {
    List<String> ports = Lists.newArrayList("8081", "8082", "8083", "8084", "8085");
    for (String port : ports) {
      try {
        int statusCode =
            Request.Head("http://localhost:" + port + "/ivy/info/index.jsp")
                .execute()
                .returnResponse()
                .getStatusLine()
                .getStatusCode();
        if (statusCode == 200) {
          return Optional.of("http://localhost:" + port);
        }
      } catch (Exception ex) {
        LOG.info(
            "Ivy Engine is not running on port: " + port + ", got exception: " + ex.getMessage());
      }
    }
    return Optional.empty();
  }

  public boolean libraryDirectoryExists() {
    PreferenceService.State preferences = preferenceService.getState();
    File libraryDirectory =
        new File(
            preferences.getIvyEngineDirectory()
                + preferences.getIvyEngineDefinition().getLibraryDirectory());
    return libraryDirectory.isDirectory();
  }
}
