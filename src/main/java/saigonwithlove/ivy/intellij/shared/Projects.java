package saigonwithlove.ivy.intellij.shared;

import com.google.common.base.Preconditions;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.apache.maven.model.Model;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Projects {

  @NotNull
  public static List<Model> getMavenModels(Project project) {
    return Arrays.stream(
            ModuleManager.getInstance(Preconditions.checkNotNull(project)).getModules())
        .map(Modules::toMavenModel)
        .flatMap(modelOpt -> modelOpt.map(Stream::of).orElseGet(Stream::empty))
        .collect(Collectors.toList());
  }
}
