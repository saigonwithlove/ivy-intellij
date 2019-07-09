package saigonwithlove.ivy.intellij.engine;

import com.google.common.collect.ImmutableList;
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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.LocalFileFinder;

@NoArgsConstructor
public class IvyEngineService {
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
  private static final List<String> IVY_PATHS =
      ImmutableList.of(
          "/system/lib/boot/", "/system/plugins/", "/system/configuration/org.eclipse.osgi/");
  private static final Logger LOG =
      Logger.getInstance("#" + IvyEngineService.class.getCanonicalName());

  public void startIvyEngine(@NotNull String ivyEngineDirectory, @NotNull Project project) {
    GeneralCommandLine commandLine =
        new GeneralCommandLine(ivyEngineDirectory + "/bin/AxonIvyEngine");
    commandLine.setWorkDirectory(ivyEngineDirectory);
    commandLine.addParameters("start");
    try {
      ExecutionEnvironment environment =
          ExecutionEnvironmentBuilder.create(
                  project,
                  DefaultRunExecutor.getRunExecutorInstance(),
                  new IvyEngineRunProfile(commandLine))
              .build();
      environment.setExecutionId(123456789);
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
      File libraryDirectory = new File(path);
      if (libraryDirectory.isDirectory()) {
        for (File jar : FileUtils.listFiles(libraryDirectory, new String[] {"jar"}, true)) {
          if (excludePaths == null
              || excludePaths.stream().noneMatch(jar.getAbsolutePath()::contains)) {
            jars.add(LocalFileFinder.findFile(jar.getAbsolutePath()));
          }
        }
      } else {
        throw new IllegalArgumentException(
            libraryDirectory.getAbsolutePath() + " is not a directory.");
      }
    }
    return jars;
  }

  private List<String> getLibraryPaths(
      @NotNull String ivyEngineDirectory, @NotNull List<String> libraryPaths) {
    return libraryPaths.stream()
        .map(path -> ivyEngineDirectory + path)
        .collect(Collectors.toList());
  }

  private Library createUniqueLibrary(LibraryTable libraryTable, String libraryName) {
    return Optional.ofNullable(libraryTable.getLibraryByName(libraryName))
        .map(library -> {
          libraryTable.removeLibrary(library);
          return libraryTable.createLibrary(libraryName);
        })
        .orElseGet(() -> libraryTable.createLibrary(libraryName));
  }
}
