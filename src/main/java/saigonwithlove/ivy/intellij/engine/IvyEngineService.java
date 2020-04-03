package saigonwithlove.ivy.intellij.engine;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import java.io.File;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.settings.PreferenceService;

public class IvyEngineService {
  private static final Logger LOG =
      Logger.getInstance("#" + IvyEngineService.class.getCanonicalName());
  private Project project;
  private PreferenceService preferenceService;
  private IvyEngineRuntime runtime;

  public IvyEngineService(@NotNull Project project) {
    this.project = project;
    this.preferenceService = ServiceManager.getService(project, PreferenceService.class);
  }

  @NotNull
  public IvyEngineRuntime getRuntime() {
    if (Objects.isNull(this.runtime)
        || this.runtime.getStatus() == IvyEngineRuntime.Status.STOPPED) {
      PreferenceService.Cache cache = preferenceService.getCache();
      this.runtime =
          new IvyEngineRuntime(
              project, cache.getIvyEngineDirectory(), cache.getIvyEngineDefinition());
    }
    return this.runtime;
  }

  public void addLibraries() {
    LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable();
    preferenceService
        .getCache()
        .getIvyEngineDefinition()
        .getLibraries()
        .forEach(
            ivyLibrary ->
                IvyLibraries.defineLibrary(
                    preferenceService.getCache().getIvyEngineDirectory(),
                    libraryTable,
                    ivyLibrary));
  }

  public boolean libraryDirectoryExists() {
    PreferenceService.Cache cache = preferenceService.getCache();
    File libraryDirectory =
        new File(
            cache.getIvyEngineDirectory() + cache.getIvyEngineDefinition().getLibraryDirectory());
    return libraryDirectory.isDirectory();
  }

  public boolean isValidIvyEngine() {
    String ivyEngineDirectory = preferenceService.getCache().getIvyEngineDirectory();
    if (StringUtils.isBlank(ivyEngineDirectory)) {
      return false;
    }

    try {
      IvyEngineVersions.parseVersion(ivyEngineDirectory);
      return true;
    } catch (NoSuchElementException ex) {
      LOG.warn(ex);
      return false;
    }
  }
}
