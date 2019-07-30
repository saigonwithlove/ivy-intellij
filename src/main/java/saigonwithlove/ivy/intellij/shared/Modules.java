package saigonwithlove.ivy.intellij.shared;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import java.io.FileReader;
import java.io.IOException;
import java.text.Collator;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import lombok.experimental.UtilityClass;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Modules {
  private static final Logger LOG = Logger.getInstance("#" + Modules.class.getCanonicalName());

  public static boolean isIvyModule(Module module) {
    return toMavenModel(module)
        .map(Model::getPackaging)
        .map("iar"::equalsIgnoreCase)
        .orElse(Boolean.FALSE);
  }

  public static Optional<Model> toMavenModel(Module module) {
    try {
      return Optional.of(
          new MavenXpp3Reader()
              .read(
                  new FileReader(
                      module.getModuleFile().getParent().getCanonicalPath() + "/pom.xml")));
    } catch (IOException | XmlPullParserException ex) {
      LOG.error("Could not read pom.xml.", ex);
      return Optional.empty();
    }
  }

  public static int compareByName(Module a, Module b) {
    return Collator.getInstance().compare(a.getName(), b.getName());
  }

  @NotNull
  public static List<Dependency> getMissingIvyDependencies(Module module, List<Model> models) {
    Optional<Model> modelOpt = Modules.toMavenModel(module);
    return modelOpt
        .map(
            model ->
                model.getDependencies().stream()
                    .filter(dependency -> "iar".equalsIgnoreCase(dependency.getType()))
                    .filter(dependency -> models.stream().noneMatch(resolveDependency(dependency)))
                    .collect(Collectors.toList()))
        .orElse(Collections.emptyList());
  }

  @NotNull
  private static Predicate<Model> resolveDependency(Dependency dependency) {
    return model -> {
      try {
        return dependency.getGroupId().equals(model.getGroupId())
            && dependency.getArtifactId().equals(model.getArtifactId())
            && VersionRange.createFromVersionSpec(dependency.getVersion())
                .containsVersion(new DefaultArtifactVersion(model.getVersion()));
      } catch (InvalidVersionSpecificationException ex) {
        LOG.error(ex);
        return false;
      }
    };
  }
}
