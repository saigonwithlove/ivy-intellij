package saigonwithlove.ivy.intellij.engine;

import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.shared.Configuration;
import saigonwithlove.ivy.intellij.shared.IvyModule;

public interface IvyEngine {
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
  Single<IvyEngine> stop();

  /**
   * After creating Ivy Engine, the caller should execute initialize() to prepare needed information
   * for running with Ivy Plugin.
   */
  void initialize();

  /**
   * Deploy Ivy Module to the running Ivy Engine.
   *
   * @param ivyModule to be deployed
   */
  void deployIvyModule(@NotNull IvyModule ivyModule);

  /**
   * Check if Ivy Module is deployed in running Ivy Engine.
   *
   * @param ivyModule to be checked
   * @return true if found a Process Model match with Ivy Module name
   */
  boolean isIvyModuleDeployed(@NotNull IvyModule ivyModule);

  default boolean isIvyModuleNotDeployed(@NotNull IvyModule ivyModule) {
    return !this.isIvyModuleDeployed(ivyModule);
  }

  /**
   * Build IntelliJ Global Libraries of Ivy Framework, it will help IntelliJ recognize Ivy API in
   * Ivy Modules.
   */
  void buildIntellijLibraries();

  /** Subscribe to change of Port and Status of this Ivy Engine. */
  void subscribe(@NotNull Observer<IvyEngine> observer);

  void updateGlobalVariable(@NotNull Configuration configuration);

  void updateServerProperty(@NotNull Configuration configuration);

  Map<String, Configuration> getServerProperties();

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
  List<String> getProcessModels();

  /** Return localhost:port if the Engine is RUNNING, otherwise return empty. */
  @NotNull
  Optional<URL> getUrl();

  enum Status {
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
  }
}
