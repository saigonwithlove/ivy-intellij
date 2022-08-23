package saigonwithlove.ivy.intellij.engine;

import com.intellij.openapi.project.Project;
import java.util.NoSuchElementException;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.apache.maven.artifact.versioning.ArtifactVersion;

/**
 * IvyEngineFactory will decouple link IvyEngine interface and its implementations, the instance
 * creation is implemented in this class instead of a static method in IvyEngine interface. This
 * class will be a service within Ivy Plugin.
 */
@Getter
public class IvyEngineFactory {

  @NonNull private String directory;
  @NonNull private Project project;
  private ArtifactVersion version;
  private IvyEngineDefinition definition;

  @Builder
  public IvyEngineFactory(String ivyEngineDirectory, Project project) {
    this.project = project;
    this.changeDirectory(ivyEngineDirectory);
  }

  public void changeDirectory(String ivyEngineDirectory) {
    this.directory = ivyEngineDirectory;
    this.version = IvyEngineVersions.parseVersion(ivyEngineDirectory);
    this.definition = IvyEngineDefinition.fromVersion(this.version);
  }

  public IvyEngine newEngine() {
    if (this.version.getMajorVersion() == 7) {
      return Ivy7Engine.builder()
          .directory(this.directory)
          .version(this.version)
          .definition(this.definition)
          .project(this.project)
          .build();
    } else if (this.version.getMajorVersion() == 8) {
      return Ivy8Engine.builder()
          .directory(this.directory)
          .version(this.version)
          .definition(this.definition)
          .project(this.project)
          .build();
    } else if (this.version.getMajorVersion() == 6) {
      return Ivy6Engine.builder()
          .directory(this.directory)
          .version(this.version)
          .definition(this.definition)
          .project(this.project)
          .build();
    }
    throw new NoSuchElementException("Could not create Engine with version: " + this.version);
  }
}
