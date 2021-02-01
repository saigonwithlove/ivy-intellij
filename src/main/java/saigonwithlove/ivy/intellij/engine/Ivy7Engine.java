package saigonwithlove.ivy.intellij.engine;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import lombok.Builder;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.devtool.IvyDevtools;

public class Ivy7Engine extends BaseIvyEngine implements IvyEngine {
  private static final Logger LOG = Logger.getInstance("#" + Ivy7Engine.class.getCanonicalName());

  @Builder
  public Ivy7Engine(
      @NotNull String directory,
      @NotNull ArtifactVersion version,
      @NotNull IvyEngineDefinition definition,
      @NotNull Project project) {
    super(directory, version, definition, project);
  }

  @Override
  public void initialize() {
    // Clean up old Process Models
    this.getProcessModels().stream()
        .filter(
            processModel -> !Arrays.asList("JsfWorkflowUi", "ivy-devtool").contains(processModel))
        .forEach(
            processModel -> {
              try {
                FileUtils.deleteDirectory(
                    new File(
                        this.getDirectory()
                            + "/"
                            + this.getDefinition().getDefaultApplicationDirectory()
                            + "/"
                            + processModel));
              } catch (IOException ex) {
                LOG.error("Could not delete Process Model: " + processModel, ex);
              }
            });

    IvyDevtools.installIvyDevtool(this);
  }
}
