package saigonwithlove.ivy.intellij.engine;

import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.codehaus.plexus.util.DirectoryScanner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.io.LocalFileFinder;

@UtilityClass
public class IvyLibraries {
  public static void defineLibrary(
      @NotNull String ivyEngineDirectory,
      @NotNull LibraryTable libraryTable,
      @NotNull IvyLibrary ivyLibrary) {
    Library.ModifiableModel modifiableModel =
        createUniqueLibrary(libraryTable, ivyLibrary.getName()).getModifiableModel();
    getJars(ivyEngineDirectory, ivyLibrary.getPaths(), ivyLibrary.getExcludedPaths())
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
      @NotNull String ivyEngineDirectory,
      @NotNull List<String> paths,
      @Nullable List<String> excludePaths) {
    DirectoryScanner scanner = new DirectoryScanner();
    scanner.setBasedir(ivyEngineDirectory);
    scanner.setIncludes(paths.toArray(new String[0]));
    scanner.setExcludes(excludePaths != null ? excludePaths.toArray(new String[0]) : null);
    scanner.scan();
    return Arrays.stream(scanner.getIncludedFiles())
        .map(relativePath -> ivyEngineDirectory + "/" + relativePath)
        .map(LocalFileFinder::findFile)
        .collect(Collectors.toList());
  }
}
