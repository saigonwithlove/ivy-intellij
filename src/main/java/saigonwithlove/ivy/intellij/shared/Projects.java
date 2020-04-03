package saigonwithlove.ivy.intellij.shared;

import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Projects {
  @NotNull
  public static List<IvyModule> getIvyModules(@NotNull Project project) {
    return Arrays.stream(ModuleManager.getInstance(project).getModules())
        .map(Modules::toIvyModule)
        .flatMap(moduleOpt -> moduleOpt.map(Stream::of).orElseGet(Stream::empty))
        .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
        .collect(Collectors.toList());
  }
}
