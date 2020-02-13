package saigonwithlove.ivy.intellij.engine;

import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.io.LocalFileFinder;

@UtilityClass
public class IvyLibraries {
  private static final String JAR_EXTENSION = "jar";

  public static void defineLibrary(
      @NotNull String ivyEngineDirectory,
      @NotNull LibraryTable libraryTable,
      @NotNull IvyLibrary ivyLibrary) {
    Library.ModifiableModel modifiableModel =
        createUniqueLibrary(libraryTable, ivyLibrary.getName()).getModifiableModel();
    List<String> paths =
        Optional.ofNullable(ivyLibrary.getPaths())
            .map(path -> getLibraryPaths(ivyEngineDirectory, path))
            .orElse(Collections.emptyList());
    List<String> excludedPaths =
        Optional.ofNullable(ivyLibrary.getExcludedPaths())
            .map(path -> getLibraryPaths(ivyEngineDirectory, path))
            .orElse(Collections.emptyList());
    getJars(paths, excludedPaths)
        .forEach(
            jar -> modifiableModel.addRoot("jar://" + jar.getPath() + "!/", OrderRootType.CLASSES));
    modifiableModel.commit();
  }

  @NotNull
  private static Library createUniqueLibrary(
      @NotNull LibraryTable libraryTable, @NotNull String libraryName) {
    return Optional.ofNullable(libraryTable.getLibraryByName(libraryName))
        .map(
            library -> {
              libraryTable.removeLibrary(library);
              return libraryTable.createLibrary(libraryName);
            })
        .orElseGet(() -> libraryTable.createLibrary(libraryName));
  }

  @NotNull
  private static List<VirtualFile> getJars(
      @NotNull List<String> paths, @NotNull List<String> excludePaths) {
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

  private static boolean isNotExcluded(@NotNull File jar, @NotNull List<String> excludePaths) {
    return excludePaths.stream().noneMatch(jar.getAbsolutePath()::contains);
  }

  @NotNull
  private static List<String> getLibraryPaths(
      @NotNull String ivyEngineDirectory, @NotNull List<String> libraryPaths) {
    return libraryPaths.stream()
        .map(path -> ivyEngineDirectory + path)
        .collect(Collectors.toList());
  }
}
