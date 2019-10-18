package saigonwithlove.ivy.intellij.shared;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import java.io.File;
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
    return Optional.of(module)
        .filter(Modules::isMavenModel)
        .flatMap(Modules::toMavenModel)
        .map(Modules::isIvyModel)
        .orElse(Boolean.FALSE);
  }

  public static boolean isIvyModel(Model model) {
    return Optional.of(model)
        .map(Model::getPackaging)
        .map("iar"::equalsIgnoreCase)
        .orElse(Boolean.FALSE);
  }

  public static Optional<Model> toMavenModel(Module module) {
    try {
      return Optional.of(new MavenXpp3Reader().read(new FileReader(getPomPath(module))));
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

  public static boolean isMavenModel(Module module) {
    return Optional.of(module)
        .map(m -> getPomPath(m))
        .map(path -> new File(path))
        .map(File::isFile)
        .orElse(Boolean.FALSE);
  }

  @NotNull
  private static String getPomPath(Module module) {
    return ModuleRootManager.getInstance(module).getContentRoots()[0].getCanonicalPath()
        + "/pom.xml";
  }
}
