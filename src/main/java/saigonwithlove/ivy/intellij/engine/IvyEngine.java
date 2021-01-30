package saigonwithlove.ivy.intellij.engine;

import com.google.common.base.Preconditions;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.devtool.IvyDevtools;
import saigonwithlove.ivy.intellij.shared.Configuration;
import saigonwithlove.ivy.intellij.shared.IvyModule;

public interface IvyEngine {
  Logger LOG = Logger.getInstance("#" + IvyEngine.class.getCanonicalName());

  /**
   * Async method, the Engine need some time to start. Should notify when it started. If the Engine
   * is RUNNING, notify immediately. If the Engine is STOPPED, change to status STARTING, then
   * change to RUNNING when it's ready. Change status to STOPPED when could not start the Engine.
   */
  @NotNull
  Single<IvyEngine> start();

  /**
   * Async method, use this method to gracefully stop the Engine. Should notify when it stopped. If
   * the Engine is STOPPED, notify immediately. If the Engine is RUNNING, change the status to
   * STOPPING, then change to STOPPED when done.
   */
  @NotNull
  Observable<IvyEngine> stop();

  void initialize();

  default void deployIvyModule(@NotNull IvyModule ivyModule) {
    IvyDevtools.deployIvyModule(this, ivyModule);
  }

  default void buildIntellijLibraries() {
    LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable();
    Optional.ofNullable(LocalFileSystem.getInstance().findFileByPath(this.getDirectory()))
        .ifPresent(directory -> directory.refresh(false, true));
    this.getDefinition()
        .getLibraries()
        .forEach(
            ivyLibrary ->
                IvyLibraries.defineLibrary(this.getDirectory(), libraryTable, ivyLibrary));
  }

  @SneakyThrows
  @NotNull
  default URL newEngineUrl(int port) {
    return new URL("http://localhost:" + port);
  }

  /** Subscribe to change of Port and Status of this Ivy Engine. */
  void subscribe(@NotNull Observer<IvyEngine> observer);

  default boolean isIvyModuleDeployed(@NotNull IvyModule ivyModule) {
    Optional<VirtualFile> processModelVersionDirectoryOpt =
        Optional.ofNullable(
            LocalFileSystem.getInstance()
                .refreshAndFindFileByPath(
                    getDefaultApplicationDirectory() + "/" + ivyModule.getName() + "/1"));
    return processModelVersionDirectoryOpt.map(VirtualFile::isDirectory).orElse(Boolean.FALSE);
  }

  default boolean isIvyModuleNotDeployed(@NotNull IvyModule ivyModule) {
    return !this.isIvyModuleDeployed(ivyModule);
  }

  default void updateGlobalVariable(@NotNull Configuration configuration) {
    IvyDevtools.updateGlobalVariable(this, configuration);
  }

  default void updateServerProperty(@NotNull Configuration configuration) {
    IvyDevtools.updateServerProperty(this, configuration);
  }

  default Map<String, Configuration> getServerProperties() {
    return IvyDevtools.getServerProperties(this);
  }

  /**
   * The default application in which Ivy Modules will be deployed.
   *
   * @return the path of default application within Ivy Engine directory
   */
  String getDefaultApplicationDirectory();

  /** Return the current status of the Engine. Should be STOPPED, STARTING, RUNNING, STOPPING. */
  @NotNull
  Status getStatus();

  /** Return the current port of running Engine, otherwise, return -1 */
  int getPort();

  /** Return the Ivy Engine version */
  @NotNull
  ArtifactVersion getVersion();

  /** Return the Ivy Engine Definition */
  @NotNull
  IvyEngineDefinition getDefinition();

  /** Return Ivy Engine Directory */
  @NotNull
  String getDirectory();

  @NotNull
  default List<String> getProcessModels() {
    return Optional.ofNullable(
            LocalFileSystem.getInstance()
                .refreshAndFindFileByPath(getDefaultApplicationDirectory())) // get Portal directory
        .map(VirtualFile::getChildren) // get all Process Models as array
        .map(Arrays::stream) // convert to Stream
        .orElse(Stream.empty()) // return empty Stream if directory or Process Model is null
        .filter(child -> child.isDirectory() && !"files".equals(child.getName()))
        .map(VirtualFile::getName) // convert from VirtualFile to String (Process Model name)
        .collect(Collectors.toList());
  }

  /** Return localhost:port if the Engine is RUNNING, otherwise return empty. */
  @NotNull
  default Optional<URL> getUrl() {
    if (this.getPort() == -1) {
      return Optional.empty();
    }
    return Optional.of(newEngineUrl(this.getPort()));
  }

  @NotNull
  default String getCompatibleJavaHome(@NotNull JavaSdkVersion jdkVersion) {
    Supplier<RuntimeException> noJdkFoundExceptionSupplier =
        () ->
            new NoSuchElementException(
                MessageFormat.format("Could not find JDK version: {0}", jdkVersion));
    return Arrays.stream(ProjectJdkTable.getInstance().getAllJdks())
        .filter(sdk -> "JavaSDK".equals(sdk.getSdkType().getName()))
        .filter(
            sdk ->
                jdkVersion
                    == JavaSdkVersion.fromVersionString(
                        Preconditions.checkNotNull(sdk.getVersionString())))
        .findFirst()
        .map(Sdk::getHomePath)
        .orElseThrow(noJdkFoundExceptionSupplier);
  }

  enum Status {
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
  }
}
