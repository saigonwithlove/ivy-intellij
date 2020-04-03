package saigonwithlove.ivy.intellij.shared;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.maven.model.Model;
import org.jetbrains.annotations.NotNull;

@Getter
@AllArgsConstructor
public class IvyModule {
  private Module module;
  private Model mavenModel;

  @NotNull
  public List<Configuration> getGlobalVariables() {
    return Optional.ofNullable(
            this.getContentRoot().findFileByRelativePath("config/GlobalVariables"))
        .map(VirtualFile::getChildren)
        .map(Arrays::stream)
        .orElse(Stream.empty())
        .filter(VirtualFile::isDirectory)
        .map(Configurations::parseGlobalVariable)
        .collect(Collectors.toList());
  }

  @NotNull
  public VirtualFile getContentRoot() {
    return Modules.getContentRoot(module);
  }

  @NotNull
  public String getName() {
    return this.module.getName();
  }
}
