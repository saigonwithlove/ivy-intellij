package saigonwithlove.ivy.intellij.engine;

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
  IVY6(new DefaultArtifactVersion("6.0.0"), Arrays.asList(Ivy6Library.values()), "/lib") {
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
      "/system/configuration/org.eclipse.osgi") {
    @NotNull
    @Override
    public String getStartCommand() {
      if (SystemUtils.IS_OS_WINDOWS) {
        return "/bin/AxonIvyEngine.exe";
      }
      return "/bin/AxonIvyEngine";
    }
  };

  private ArtifactVersion version;
  private List<IvyLibrary> libraries;
  private String libraryDirectory;

  @NotNull
  public abstract String getStartCommand();

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
