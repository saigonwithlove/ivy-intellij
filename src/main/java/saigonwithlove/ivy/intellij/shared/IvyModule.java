package saigonwithlove.ivy.intellij.shared;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.maven.model.Model;
import org.jetbrains.annotations.NotNull;

@RequiredArgsConstructor
public class IvyModule {
  @NonNull @Getter private Module module;
  private Model mavenModel;
  private long mavenModelTimestamp;

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
    return Modules.getContentRoot(module)
        .orElseThrow(
            () ->
                new NoSuchElementException(
                    "Could not find content root of module:" + module.getName()));
  }

  @NotNull
  public String getName() {
    return this.module.getName();
  }

  public Model getMavenModel() {
    if (this.mavenModel == null || isPomModified(this.module, this.mavenModelTimestamp)) {
      this.mavenModel =
          Modules.toMavenModel(this.module)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Ivy Module " + this.module.getName() + " should have maven model."));
      this.mavenModelTimestamp =
          Modules.getPomFile(module)
              .map(VirtualFile::getTimeStamp)
              .orElseThrow(
                  () ->
                      new IllegalArgumentException(
                          "Could not find pom.xml of Ivy Module: " + this.module.getName()));
    }
    return this.mavenModel;
  }

  private boolean isPomModified(@NonNull Module module, long lastTimestamp) {
    return Modules.getPomFile(module)
        .map(pom -> pom.getTimeStamp() != lastTimestamp)
        .orElse(Boolean.FALSE);
  }
}
