package saigonwithlove.ivy.intellij.engine;

import com.intellij.openapi.projectRoots.JavaSdkVersion;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.SystemUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public enum IvyEngineDefinition {
  IVY6(
      new DefaultArtifactVersion("6.0.0"),
      Arrays.asList(Ivy6Library.values()),
      "lib",
      "applications/Portal",
      JavaSdkVersion.JDK_1_8) {
    @NotNull
    @Override
    public String getStartCommand() {
      if (SystemUtils.IS_OS_WINDOWS) {
        return "/bin/AxonIvyEngine.exe";
      }
      return "/bin/AxonIvyEngine.sh";
    }
  },
  IVY7(
      new DefaultArtifactVersion("7.0.0"),
      Arrays.asList(Ivy7Library.values()),
      "system/configuration/org.eclipse.osgi",
      "system/applications/Portal",
      JavaSdkVersion.JDK_1_8),
  IVY8(
      new DefaultArtifactVersion("8.0.0"),
      Arrays.asList(Ivy8Library.values()),
      "system/configuration/org.eclipse.osgi",
      "work/demo-applications/demo-portal",
      JavaSdkVersion.JDK_11);

  private final ArtifactVersion version;
  private final List<IvyLibrary> libraries;
  private final String osgiDirectory;
  private final String defaultApplicationDirectory;
  private final JavaSdkVersion jdkVersion;

  @NotNull
  public String getStartCommand() {
    if (SystemUtils.IS_OS_WINDOWS) {
      return "bin/AxonIvyEngine.exe";
    }
    return "bin/AxonIvyEngine";
  }

  /**
   * @return relative path of Ivy Devtool Process Model Version from ivyEngineDirectory +
   *     defaultApplicationDirectory + "/" + ivyDevtoolProcessModelVersionPath
   */
  public @NotNull String getIvyDevtoolProcessModelVersionPath() {
    if (this.version.getMajorVersion() < 8) {
      return "ivy-devtool/1";
    } else {
      return "ivy-devtool/1.zip";
    }
  }

  @NotNull
  public static IvyEngineDefinition fromVersion(@NotNull ArtifactVersion version) {
    Supplier<NoSuchElementException> exceptionSupplier =
        () ->
            new NoSuchElementException(
                MessageFormat.format(
                    "Ivy Engine version {0} is not supported.", version.toString()));
    return Arrays.stream(IvyEngineDefinition.values())
        .filter(item -> item.getVersion().getMajorVersion() == version.getMajorVersion())
        .findFirst()
        .orElseThrow(exceptionSupplier);
  }
}
