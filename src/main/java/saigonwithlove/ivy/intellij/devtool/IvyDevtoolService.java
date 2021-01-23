package saigonwithlove.ivy.intellij.devtool;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saigonwithlove.ivy.intellij.engine.IvyEngine;
import saigonwithlove.ivy.intellij.engine.IvyEngineDefinition;
import saigonwithlove.ivy.intellij.mirror.FileSyncProcessor;
import saigonwithlove.ivy.intellij.settings.CacheObserver;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.IvyModule;
import saigonwithlove.ivy.intellij.shared.Modules;

public class IvyDevtoolService {
  private static final Logger LOG =
      Logger.getInstance("#" + IvyDevtoolService.class.getCanonicalName());
  private static final String IVY_DEVTOOL_PACKAGE_URL =
      "https://github.com/saigonwithlove/ivy-devtool/releases/download/ivy7/ivy-devtool.iar";
  private static final String IVY_DEVTOOL_URL =
      "/ivy/pro/Portal/ivy-devtool/16AE38ED14569A2A/engine.ivp";
  private static final String IVY_DEVTOOL_VERSION_RANGE = "[0.2.3,)";

  private Project project;
  private Optional<IvyEngine> ivyEngineOpt;

  public IvyDevtoolService(Project project) {
    this.project = project;

    PreferenceService preferenceService =
        ServiceManager.getService(project, PreferenceService.class);
    preferenceService
        .asObservable()
        .map(PreferenceService.State::getIvyEngine)
        .subscribe(
            new CacheObserver<>(
                "Update Ivy Engine in IvyDevtoolService.",
                ivyEngine -> {
                  this.ivyEngineOpt = Optional.ofNullable(ivyEngine);
                }));
  }

  public boolean exists() {
    if (!this.ivyEngineOpt.isPresent()) {
      return false;
    }

    IvyEngine engine = this.ivyEngineOpt.get();
    Optional<VirtualFile> ivyDevtoolDirectoryOpt =
        Optional.ofNullable(
            LocalFileSystem.getInstance()
                .findFileByPath(
                    getIvyDevtoolDirectory(engine.getDirectory(), engine.getDefinition())));
    ivyDevtoolDirectoryOpt.ifPresent(
        ivyDevtoolDirectory -> ivyDevtoolDirectory.refresh(false, false));
    return ivyDevtoolDirectoryOpt.map(VirtualFile::isDirectory).orElse(Boolean.FALSE);
  }

  public boolean notExists() {
    return !exists();
  }

  public boolean isUpdated() {
    if (!this.ivyEngineOpt.isPresent()) {
      return false;
    }

    IvyEngine engine = this.ivyEngineOpt.get();
    Optional<VirtualFile> pomOpt =
        Optional.ofNullable(
            LocalFileSystem.getInstance()
                .findFileByPath(
                    getIvyDevtoolDirectory(engine.getDirectory(), engine.getDefinition())
                        + "/pom.xml"));
    pomOpt.ifPresent(pom -> pom.refresh(false, false));
    return pomOpt
        .flatMap(Modules::toMavenModel)
        .map(model -> new DefaultArtifactVersion(model.getVersion()))
        .map(
            version -> {
              try {
                return VersionRange.createFromVersionSpec(IVY_DEVTOOL_VERSION_RANGE)
                    .containsVersion(version);
              } catch (InvalidVersionSpecificationException ex) {
                LOG.error("Could not evaluate version of Ivy Devtool: " + version, ex);
                return false;
              }
            })
        .orElse(Boolean.FALSE);
  }

  public boolean isOutdated() {
    return !isUpdated();
  }

  public void install(ProgressIndicator indicator) {
    if (!this.ivyEngineOpt.isPresent()) {
      return;
    }

    IvyEngine engine = this.ivyEngineOpt.get();
    indicator.setText(IvyBundle.message("tasks.installIvyDevtool.progress.downloading"));
    indicator.setFraction(0.1);
    File ivyDevtoolPackage = downloadIvyDevtool(IVY_DEVTOOL_PACKAGE_URL);
    indicator.setText(IvyBundle.message("tasks.installIvyDevtool.progress.installing"));
    indicator.setFraction(0.5);
    String ivyDevtoolDirectoryText =
        engine.getDirectory()
            + engine.getDefinition().getApplicationDirectory()
            + "/Portal/ivy-devtool/1";
    extract(ivyDevtoolPackage, ivyDevtoolDirectoryText);
    ivyDevtoolPackage.delete();
    indicator.setText(IvyBundle.message("tasks.installIvyDevtool.progress.complete"));
    indicator.setFraction(1);
  }

  @NotNull
  private String getIvyDevtoolDirectory(
      @NotNull String ivyEngineDirectory, @NotNull IvyEngineDefinition ivyEngineDefinition) {
    return ivyEngineDirectory
        + ivyEngineDefinition.getApplicationDirectory()
        + "/Portal/ivy-devtool/1";
  }

  @NotNull
  private File downloadIvyDevtool(@NotNull String ivyDevtoolUrlText) {
    try {
      URL ivyDevtoolUrl = new URL(ivyDevtoolUrlText);
      Path downloadedIvyDevtoolPackage = Files.createTempFile("ivy-devtool", ".iar");
      FileUtils.copyURLToFile(ivyDevtoolUrl, downloadedIvyDevtoolPackage.toFile());
      return downloadedIvyDevtoolPackage.toFile();
    } catch (IOException ex) {
      throw new RuntimeException("Could not download Ivy Devtool package.", ex);
    }
  }

  @NotNull
  private File extract(@NotNull File ivyDevtoolPackage, @NotNull String ivyDevtoolDirectoryText) {
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(ivyDevtoolPackage))) {
      File ivyDevtoolDirectory = new File(ivyDevtoolDirectoryText);
      FileUtils.forceMkdir(ivyDevtoolDirectory);
      byte[] buffer = new byte[1024];
      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        String fileName = zipEntry.getName();
        File newFile = new File(ivyDevtoolDirectoryText + "/" + fileName);
        if (zipEntry.isDirectory() && !newFile.exists()) {
          newFile.mkdir();
        } else if (!zipEntry.isDirectory()) {
          FileOutputStream fos = new FileOutputStream(newFile);
          int len;
          while ((len = zis.read(buffer)) > 0) {
            fos.write(buffer, 0, len);
          }
          fos.close();
        }
        zipEntry = zis.getNextEntry();
      }
      zis.closeEntry();
      return ivyDevtoolDirectory;
    } catch (IOException ex) {
      throw new RuntimeException("Could not extract Ivy Devtool package.", ex);
    }
  }

  public void reloadModule(@NotNull IvyModule ivyModule) {
    try {
      String baseIvyEngineUrl =
          getIvyEngineUrl()
              .orElseThrow(() -> new NoSuchElementException("Could not get baseIvyEngineUrl."));
      URI reloadModuleUri =
          new URIBuilder(baseIvyEngineUrl + IVY_DEVTOOL_URL)
              .addParameter("command", "module$reload")
              .addParameter("pm", ivyModule.getName())
              .addParameter("pmv", "1")
              .build();

      Request.Get(reloadModuleUri).execute().returnContent();
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void compileModule(@NotNull IvyModule ivyModule, CompileStatusNotification notification) {
    CompilerManager.getInstance(project).make(ivyModule.getModule(), notification);
  }

  public void deployModule(@NotNull IvyModule ivyModule, @Nullable Task nextTask) {
    if (!this.ivyEngineOpt.isPresent()) {
      return;
    }

    IvyEngine engine = this.ivyEngineOpt.get();
    Path source =
        Paths.get(Preconditions.checkNotNull(ivyModule.getContentRoot().getCanonicalPath()));
    Path target =
        Paths.get(
            engine.getDirectory() + "/system/applications/Portal/" + ivyModule.getName() + "/1");
    FileSyncProcessor.Options options = new FileSyncProcessor.Options();
    FileSyncProcessor fileSyncProcessor = new FileSyncProcessor();
    FileSyncProcessor.UserInterface ui =
        new FileSyncProcessor.UserInterface() {

          @Override
          public void writeInfo(String s) {}

          @Override
          public void writeInfoTicker() {}

          @Override
          public void endInfoTicker() {}

          @Override
          public void writeDebug(String s) {}

          @Override
          public void listItem(String relativePath, FileSyncProcessor.DiffStatus diffStatus) {}
        };
    FileSyncProcessor.Statistics statistics = new FileSyncProcessor.Statistics();
    try {
      fileSyncProcessor.main(source, target, options, ui, statistics);
      if (Objects.nonNull(nextTask)) {
        ProgressManager.getInstance().run(nextTask);
      }
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public void deployModule(@NotNull IvyModule ivyModule) {
    this.deployModule(ivyModule, null);
  }

  @NotNull
  private Optional<String> getIvyEngineUrl() {
    return this.ivyEngineOpt.map(IvyEngine::getUrl).map(String::valueOf);
  }

  public void updateGlobalVariable(@NotNull String name, @NotNull String value) {
    try {
      String baseIvyEngineUrl =
          getIvyEngineUrl()
              .orElseThrow(() -> new NoSuchElementException("Could not get baseIvyEngineUrl."));
      URI setGlobalVariableUri =
          new URIBuilder(baseIvyEngineUrl + IVY_DEVTOOL_URL)
              .addParameter("command", "global-variable$set")
              .addParameter("name", name)
              .addParameter("value", value)
              .build();

      Request.Get(setGlobalVariableUri).execute().returnContent();
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public void updateServerProperty(@NotNull String name, @NotNull String value) {
    try {
      String baseIvyEngineUrl =
          getIvyEngineUrl()
              .orElseThrow(() -> new NoSuchElementException("Could not get baseIvyEngineUrl."));
      URI setServerPropertyUri =
          new URIBuilder(baseIvyEngineUrl + IVY_DEVTOOL_URL)
              .addParameter("command", "server-property$set")
              .addParameter("name", name)
              .addParameter("value", value)
              .build();

      Request.Get(setServerPropertyUri).execute().returnContent();
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public boolean isDeployed(@NotNull IvyModule ivyModule) {
    if (!this.ivyEngineOpt.isPresent()) {
      return false;
    }

    IvyEngine engine = this.ivyEngineOpt.get();
    Optional<VirtualFile> processModelVersionDirectoryOpt =
        Optional.ofNullable(
            LocalFileSystem.getInstance()
                .findFileByPath(
                    engine.getDirectory()
                        + "/system/applications/Portal/"
                        + ivyModule.getName()
                        + "/1"));
    processModelVersionDirectoryOpt.ifPresent(
        processModelVersionDirectory -> processModelVersionDirectory.refresh(false, false));
    return processModelVersionDirectoryOpt.map(VirtualFile::isDirectory).orElse(Boolean.FALSE);
  }

  public boolean isNotDeployed(@NotNull IvyModule ivyModule) {
    return !isDeployed(ivyModule);
  }

  @NotNull
  public Map<String, String> getServerProperties() {
    try {
      String baseIvyEngineUrl =
          getIvyEngineUrl()
              .orElseThrow(() -> new NoSuchElementException("Could not get baseIvyEngineUrl."));
      URI setServerPropertyUri =
          new URIBuilder(baseIvyEngineUrl + IVY_DEVTOOL_URL)
              .addParameter("command", "server-property$get-all")
              .build();

      return (Map<String, String>)
          new ObjectMapper()
              .readValue(
                  Request.Get(setServerPropertyUri).execute().returnContent().asStream(),
                  Map.class);
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
