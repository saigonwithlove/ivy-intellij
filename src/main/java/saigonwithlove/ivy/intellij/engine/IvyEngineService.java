package saigonwithlove.ivy.intellij.engine;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.NoArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;
import org.apache.http.client.fluent.Request;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.LocalFileFinder;
import saigonwithlove.ivy.intellij.shared.GeneralRunProfile;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

@NoArgsConstructor
public class IvyEngineService {
  private static final Logger LOG =
      Logger.getInstance("#" + IvyEngineService.class.getCanonicalName());
  private static final List<String> ULC_PATHS =
      ImmutableList.of(
          "/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc/",
          "/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc_deploy/",
          "/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc_ext/");
  private static final List<String> WEB_SERVICE_CALL_PATHS =
      ImmutableList.of("/system/configuration/org.eclipse.osgi/70/0/.cp/lib/axis2-1.3_patched/");
  private static final List<String> WEB_SERVICE_PROCESS_PATHS =
      ImmutableList.of("/system/configuration/org.eclipse.osgi/72/0/.cp/lib/mvn/");
  private static final List<String> WEB_APPLICATION_PATHS =
      ImmutableList.of("/webapps/ivy/WEB-INF/lib/");
  private static final List<String> RULE_ENGINE_PATHS =
      ImmutableList.of(
          "/system/plugins/ch.ivyteam.ivy.rule.engine.libs_7.0.0.201809271142.jar",
          "/system/plugins/ch.ivyteam.ivy.rule.engine_7.0.0.201809271142.jar");
  private static final List<String> IVY_PATHS =
      ImmutableList.of(
          "/system/lib/boot/", "/system/plugins/", "/system/configuration/org.eclipse.osgi/");
  private static final String JAR_EXTENSION = "jar";

  public void startIvyEngine(@NotNull String ivyEngineDirectory, @NotNull Project project) {
    String ivyCommand = SystemUtils.IS_OS_WINDOWS ? "/bin/AxonIvyEngine.exe" : "/bin/AxonIvyEngine";
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

  public void addLibraries(@NotNull String ivyEngineDirectory) {
    LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable();
    addLibrary(
        libraryTable,
        "IVY_CONTAINER",
        getLibraryPaths(ivyEngineDirectory, IVY_PATHS),
        ImmutableList.<String>builder()
            .addAll(getLibraryPaths(ivyEngineDirectory, ULC_PATHS))
            .addAll(getLibraryPaths(ivyEngineDirectory, WEB_SERVICE_CALL_PATHS))
            .addAll(getLibraryPaths(ivyEngineDirectory, WEB_SERVICE_PROCESS_PATHS))
            .addAll(getLibraryPaths(ivyEngineDirectory, RULE_ENGINE_PATHS))
            .build());
    addLibrary(
        libraryTable,
        "WEBAPP_CONTAINER",
        getLibraryPaths(ivyEngineDirectory, WEB_APPLICATION_PATHS),
        null);
    addLibrary(libraryTable, "ULC_CONTAINER", getLibraryPaths(ivyEngineDirectory, ULC_PATHS), null);
    addLibrary(
        libraryTable,
        "WS_CALL_AXIS2_CONTAINER",
        getLibraryPaths(ivyEngineDirectory, WEB_SERVICE_CALL_PATHS),
        null);
    addLibrary(
        libraryTable,
        "WS_PROCESS_CONTAINER",
        getLibraryPaths(ivyEngineDirectory, WEB_SERVICE_PROCESS_PATHS),
        null);
    addLibrary(
        libraryTable,
        "RULE_ENGINE_CONTAINER",
        getLibraryPaths(ivyEngineDirectory, RULE_ENGINE_PATHS),
        null);
    addLibrary(
        libraryTable, "org.eclipse.jst.j2ee.internal.web.container", Collections.emptyList(), null);
  }

  private void addLibrary(
      @NotNull LibraryTable libraryTable,
      @NotNull String libraryName,
      List<String> libraryPaths,
      List<String> excludePaths) {
    Library.ModifiableModel modifiableModel =
        createUniqueLibrary(libraryTable, libraryName).getModifiableModel();
    getJars(libraryPaths, excludePaths)
        .forEach(
            jar -> modifiableModel.addRoot("jar://" + jar.getPath() + "!/", OrderRootType.CLASSES));
    modifiableModel.commit();
  }

  private List<VirtualFile> getJars(
      @NotNull List<String> paths, @Nullable List<String> excludePaths) {
    List<VirtualFile> jars = new ArrayList<>();
    for (String path : paths) {
      File libraryPath = new File(path);
      if (libraryPath.isDirectory()) {
        for (File jar : FileUtils.listFiles(libraryPath, new String[] {JAR_EXTENSION}, true)) {
          if (isNotExcluded(jar, excludePaths)) {
            jars.add(LocalFileFinder.findFile(jar.getAbsolutePath()));
          }
        }
      } else if (libraryPath.isFile()
          && JAR_EXTENSION.equals(FilenameUtils.getExtension(libraryPath.getName()))) {
        if (isNotExcluded(libraryPath, excludePaths)) {
          jars.add(LocalFileFinder.findFile(libraryPath.getAbsolutePath()));
        }
      } else {
        throw new IllegalArgumentException(
            libraryPath.getAbsolutePath() + " is not a directory or jar file.");
      }
    }
    return jars;
  }

  private boolean isNotExcluded(File jar, @Nullable List<String> excludePaths) {
    return excludePaths == null || excludePaths.stream().noneMatch(jar.getAbsolutePath()::contains);
  }

  private List<String> getLibraryPaths(
      @NotNull String ivyEngineDirectory, @NotNull List<String> libraryPaths) {
    return libraryPaths.stream()
        .map(path -> ivyEngineDirectory + path)
        .collect(Collectors.toList());
  }

  private Library createUniqueLibrary(LibraryTable libraryTable, String libraryName) {
    return Optional.ofNullable(libraryTable.getLibraryByName(libraryName))
        .map(
            library -> {
              libraryTable.removeLibrary(library);
              return libraryTable.createLibrary(libraryName);
            })
        .orElseGet(() -> libraryTable.createLibrary(libraryName));
  }

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
}
