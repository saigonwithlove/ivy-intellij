package saigonwithlove.ivy.intellij.engine;

import io.reactivex.rxjava3.core.Observer;
import java.net.URL;
import java.util.Optional;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.NotNull;

public interface IvyEngine {
  /**
   * Async method, the Engine need some time to start. Should notify when it started. If the Engine
   * is RUNNING, notify immediately. If the Engine is STOPPED, change to status STARTING, then
   * change to RUNNING when it's ready. Change status to STOPPED when could start the Engine.
   */
  void start();

  /**
   * Async method, use this method to gracefully stop the Engine. Should notify when it stopped. If
   * the Engine is STOPPED, notify immediately. If the Engine is RUNNING, change the status to
   * STOPPING, then change to STOPPED when done.
   */
  void stop();

  /** Return the current status of the Engine. Should be STOPPED, STARTING, RUNNING, STOPPING. */
  Status getStatus();

  /** Return the current port of running Engine, otherwise, return -1 */
  int getPort();

  /** Return localhost:port if the Engine is RUNNING, otherwise return empty. */
  Optional<URL> getUrl();

  ArtifactVersion getVersion();

  IvyEngineDefinition getDefinition();

  String getDirectory();

  void buildIntellijLibraries();

  /** Subscribe to change of Port and Status of this Ivy Engine. */
  void subscribe(Observer<IvyEngine> observer);

  enum Status {
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED,
  }
}
