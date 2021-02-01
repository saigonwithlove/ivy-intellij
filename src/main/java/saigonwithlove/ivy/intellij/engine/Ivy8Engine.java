package saigonwithlove.ivy.intellij.engine;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.io.File;
import java.io.IOException;
import lombok.Builder;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.devtool.IvyDevtools;

public class Ivy8Engine extends BaseIvyEngine implements IvyEngine {
  private static final Logger LOG = Logger.getInstance("#" + Ivy8Engine.class.getCanonicalName());

  @Builder
  public Ivy8Engine(
      @NotNull String directory,
      @NotNull ArtifactVersion version,
      @NotNull IvyEngineDefinition definition,
      @NotNull Project project) {
    super(directory, version, definition, project);
  }

  @Override
  public void initialize() {
    File ivyDevtoolIarPackage = IvyDevtools.downloadIar(IvyDevtools.IVY_DEVTOOL_PACKAGE_URL);
    try {
      FileUtils.copyFile(
          ivyDevtoolIarPackage,
          new File(this.getDirectory() + "/system/demo-applications/demo-portal/ivy-devtool.iar"));
      ivyDevtoolIarPackage.delete();
    } catch (IOException ex) {
      LOG.error("Could not copy Ivy Devtool IAR package to demo-portal packages.", ex);
    }
  }
}
