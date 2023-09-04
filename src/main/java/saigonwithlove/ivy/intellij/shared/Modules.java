package saigonwithlove.ivy.intellij.shared;

import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.subjects.CompletableSubject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
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
    return getPomFile(module).flatMap(Modules::toMavenModel);
  }

  private static Comparator<Module> createModuleComparator() {
    return (@NotNull Module a, @NotNull Module b) ->
        Collator.getInstance().compare(a.getName(), b.getName());
  }

  public static Comparator<IvyModule> createIvyModuleDependencyOrderComparator(
      List<IvyModule> ivyModules) {
    return (@NotNull IvyModule a, @NotNull IvyModule b) -> {
      List<Dependency> bDependencies = getTransitiveIvyDependencies(b, ivyModules);
      boolean bDependsOnA =
          bDependencies.stream().anyMatch(dependency -> resolveDependency(dependency).test(a));
      if (bDependsOnA) {
        // The module B depends on module A if A is a dependency of B. In this
        // case, we will put A before B by returning negative value (DESC).
        LOG.info(b.getName() + " depends on " + a.getName());
        return 1;
      }
      List<Dependency> aDependencies = getTransitiveIvyDependencies(a, ivyModules);
      boolean aDependsOnB =
          aDependencies.stream().anyMatch(dependency -> resolveDependency(dependency).test(b));
      if (aDependsOnB) {
        LOG.info(a.getName() + " depends on " + b.getName());
        return -1;
      }
      LOG.info(a.getName() + " and " + b.getName() + " are independent.");
      return 0;
    };
  }

  @NotNull
  public static List<Dependency> getMissingIvyDependencies(
      @NotNull IvyModule ivyModule, @NotNull List<IvyModule> ivyModules) {
    Function<Model, List<Dependency>> missingIvyDependenciesMapper =
        model ->
            model.getDependencies().stream()
                .filter(dependency -> IVY_PACKAGE_EXTENSION.equalsIgnoreCase(dependency.getType()))
                .filter(dependency -> ivyModules.stream().noneMatch(resolveDependency(dependency)))
                .collect(Collectors.toList());
    return Optional.of(ivyModule)
        .map(IvyModule::getMavenModel)
        .map(missingIvyDependenciesMapper)
        .orElse(Collections.emptyList());
  }

  /**
   * Return Ivy transitive dependencies of an Ivy module that are existing in the workspace.
   *
   * @param ivyModule the Ivy module to get Ivy transitive dependencies.
   * @param ivyModules list of Ivy modules in the workspace.
   * @return Ivy transitive dependencies in the workspace.
   */
  @NotNull
  public static List<Dependency> getTransitiveIvyDependencies(
      @NotNull IvyModule ivyModule, @NotNull List<IvyModule> ivyModules) {
    Function<Model, List<Dependency>> existingIvyDependenciesMapper =
        model ->
            model.getDependencies().stream()
                .filter(dependency -> IVY_PACKAGE_EXTENSION.equalsIgnoreCase(dependency.getType()))
                .filter(dependency -> ivyModules.stream().anyMatch(resolveDependency(dependency)))
                .collect(Collectors.toList());

    // Store all transitive Ivy dependencies.
    List<Dependency> transitiveIvyDependencies = new ArrayList<>();
    // Queue of Ivy dependencies to be processed.
    Queue<Model> processingModels = new LinkedList<>();
    processingModels.add(ivyModule.getMavenModel());
    do {
      // Current Ivy module
      Model currentModel = processingModels.remove();
      // Get all Ivy dependencies of current module.
      List<Dependency> directIvyDependencies = existingIvyDependenciesMapper.apply(currentModel);
      for (Dependency dependency : directIvyDependencies) {
        // Add to transitive dependencies when the module exists in workspace and didn't add to
        // transitive dependencies.
        // The dependencies also added to queue to get next transitive dependencies.
        if (transitiveIvyDependencies.stream()
            .noneMatch(item -> item.getArtifactId().equals(dependency.getArtifactId()))) {
          transitiveIvyDependencies.add(dependency);
          Model model =
              ivyModules.stream()
                  .map(IvyModule::getMavenModel)
                  .filter(
                      mavenModel -> mavenModel.getArtifactId().equals(dependency.getArtifactId()))
                  .findFirst()
                  .orElseThrow();
          processingModels.add(model);
        }
      }
    } while (!processingModels.isEmpty());
    LOG.info("Resolve Ivy transitive dependencies for module: " + ivyModule.getName());
    transitiveIvyDependencies.forEach(item -> LOG.info(item.getArtifactId()));
    return transitiveIvyDependencies;
  }

  @NotNull
  private static Predicate<IvyModule> resolveDependency(@NotNull Dependency dependency) {
    return ivyModule -> {
      try {
        return dependency.getGroupId().equals(ivyModule.getMavenModel().getGroupId())
            && dependency.getArtifactId().equals(ivyModule.getMavenModel().getArtifactId())
            && VersionRange.createFromVersionSpec(dependency.getVersion())
                .containsVersion(
                    new DefaultArtifactVersion(ivyModule.getMavenModel().getVersion()));
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

  public static Optional<VirtualFile> getPomFile(@NotNull Module module) {
    return getContentRoot(module).map(root -> root.findChild("pom.xml"));
  }

  public static Optional<VirtualFile> getContentRoot(@NotNull Module module) {
    VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
    if (contentRoots.length > 0) {
      return Optional.of(contentRoots[0]);
    }
    return Optional.empty();
  }

  public static Optional<IvyModule> toIvyModule(@NotNull Module module) {
    return Optional.of(module).filter(Modules::isIvyModule).map(IvyModule::new);
  }

  public static Optional<Model> toMavenModel(@NotNull VirtualFile pom) {
    try {
      return Optional.of(new MavenXpp3Reader().read(new InputStreamReader(pom.getInputStream())));
    } catch (IOException | XmlPullParserException ex) {
      LOG.error("Could not read pom.xml.", ex);
      return Optional.empty();
    }
  }

  public static Completable compile(@NotNull Project project, @NotNull IvyModule ivyModule) {
    CompletableSubject completableSubject = CompletableSubject.create();
    CompileStatusNotification notification =
        (aborted, errors, warnings, compileContext) -> {
          completableSubject.onComplete();
        };
    CompilerManager.getInstance(project).make(ivyModule.getModule(), notification);
    return completableSubject;
  }

  public static List<IvyModule> sortByDeploymentOrder(List<IvyModule> ivyModules) {
    Comparator<IvyModule> dependencyOrderComparator =
        Modules.createIvyModuleDependencyOrderComparator(ivyModules);
    List<IvyModule> sortedIvyModules = new ArrayList<>(ivyModules);
    sortedIvyModules.sort(dependencyOrderComparator);
    Collections.reverse(sortedIvyModules);
    sortedIvyModules.forEach(i -> LOG.info(i.getName()));
    return sortedIvyModules;
  }
}
