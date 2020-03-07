package saigonwithlove.ivy.intellij.engine;

import java.io.File;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class IvyEngineVersions {
  @NotNull
  public static ArtifactVersion parseVersion(@NotNull String ivyEngineDirectory) {
    String libraryFileName =
        Stream.of("/system/plugins", "/lib/ivy")
            .map(path -> new File(ivyEngineDirectory, path))
            .map(File::list)
            .filter(Objects::nonNull)
            .flatMap(Arrays::stream)
            .filter(fileName -> fileName.toLowerCase().startsWith("ch.ivyteam.util"))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Cannot find Ivy Engine library."));
    String version = libraryFileName.substring("ch.ivyteam.util".length() + 1);
    return new DefaultArtifactVersion(toReleaseVersion(version));
  }

  @NotNull
  private String toReleaseVersion(@NotNull String version) { // 6.1.0.51869 -> 6.1.0
    String[] versionParts = StringUtils.split(version, ".");
    if (ArrayUtils.isEmpty(versionParts)) {
      throw new NoSuchElementException("Connt parse Ivy Engine version.");
    }
    return StringUtils.join(versionParts, ".", 0, Math.min(versionParts.length, 3));
  }
}
