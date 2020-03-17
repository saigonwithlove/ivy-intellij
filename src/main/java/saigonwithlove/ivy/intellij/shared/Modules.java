package saigonwithlove.ivy.intellij.shared;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
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
  public static final String IVY_DEVTOOL = "ivy-devtool";
  public static final Comparator<Module> MODULE_COMPARATOR = createModuleComparator();

  private static final Logger LOG = Logger.getInstance("#" + Modules.class.getCanonicalName());
  private static final String IVY_PACKAGE_EXTENSION = "iar";

  public static boolean isIvyModule(@NotNull Module module) {
    return Optional.of(module)
        .filter(Modules::isMavenModel)
        .flatMap(Modules::toMavenModel)
        .map(Modules::isIvyModel)
        .orElse(Boolean.FALSE);
  }

  public static boolean isIvyModel(@NotNull Model model) {
    return Optional.of(model)
        .map(Model::getPackaging)
        .map(IVY_PACKAGE_EXTENSION::equalsIgnoreCase)
        .orElse(Boolean.FALSE);
  }

  @NotNull
  public static Optional<Model> toMavenModel(@NotNull Module module) {
    Optional<VirtualFile> pomOpt = getPomFile(module);
    if (pomOpt.isPresent()) {
      try {
        return Optional.of(
            new MavenXpp3Reader().read(new InputStreamReader(pomOpt.get().getInputStream())));
      } catch (IOException | XmlPullParserException ex) {
        LOG.error("Could not read pom.xml.", ex);
        return Optional.empty();
      }
    }
    return Optional.empty();
  }

  private static Comparator<Module> createModuleComparator() {
    return (@NotNull Module a, @NotNull Module b) ->
        Collator.getInstance().compare(a.getName(), b.getName());
  }

  @NotNull
  public static List<Dependency> getMissingIvyDependencies(
      @NotNull Module module, @NotNull List<Model> models) {
    Optional<Model> modelOpt = Modules.toMavenModel(module);
    Function<Model, List<Dependency>> dependencyMapper =
        model ->
            model.getDependencies().stream()
                .filter(dependency -> IVY_PACKAGE_EXTENSION.equalsIgnoreCase(dependency.getType()))
                .filter(dependency -> models.stream().noneMatch(resolveDependency(dependency)))
                .collect(Collectors.toList());
    return modelOpt.map(dependencyMapper).orElse(Collections.emptyList());
  }

  @NotNull
  private static Predicate<Model> resolveDependency(@NotNull Dependency dependency) {
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

  public static boolean isMavenModel(@NotNull Module module) {
    return Optional.of(module)
        .flatMap(Modules::getPomFile)
        .map(pom -> pom.exists() && !pom.isDirectory())
        .orElse(Boolean.FALSE);
  }

  private static Optional<VirtualFile> getPomFile(@NotNull Module module) {
    return Optional.ofNullable(
        ModuleRootManager.getInstance(module).getContentRoots()[0].findChild("pom.xml"));
  }
}
