package saigonwithlove.ivy.intellij.devtool;

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
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.mirror.FileSyncProcessor;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.IvyModule;

public class IvyDevtoolService {
  private static final Logger LOG =
      Logger.getInstance("#" + IvyDevtoolService.class.getCanonicalName());
  private static final String IVY_DEVTOOL_PACKAGE_URL =
      "https://github.com/saigonwithlove/ivy-devtool/releases/download/ivy7/ivy-devtool.iar";
  private static final String IVY_DEVTOOL_URL =
      "/ivy/pro/Portal/ivy-devtool/16AE38ED14569A2A/engine.ivp";

  private Project project;
  private PreferenceService preferenceService;
  private IvyEngineService ivyEngineService;

  public IvyDevtoolService(Project project) {
    this.project = project;
    this.preferenceService = ServiceManager.getService(project, PreferenceService.class);
    this.ivyEngineService = ServiceManager.getService(project, IvyEngineService.class);
  }

  public boolean exists() {
    return Optional.ofNullable(
            LocalFileSystem.getInstance().findFileByPath(getIvyDevtoolDirectory()))
        .map(VirtualFile::isDirectory)
        .orElse(Boolean.FALSE);
  }

  public void install(ProgressIndicator indicator) {
    indicator.setText(IvyBundle.message("tasks.installIvyDevtool.progress.downloading"));
    indicator.setFraction(0.1);
    File ivyDevtoolPackage = downloadIvyDevtool(IVY_DEVTOOL_PACKAGE_URL);
    indicator.setText(IvyBundle.message("tasks.installIvyDevtool.progress.installing"));
    indicator.setFraction(0.5);
    String ivyDevtoolDirectoryText =
        preferenceService.getCache().getIvyEngineDirectory()
            + preferenceService.getCache().getIvyEngineDefinition().getApplicationDirectory()
            + "/Portal/ivy-devtool/1";
    extract(ivyDevtoolPackage, ivyDevtoolDirectoryText);
    ivyDevtoolPackage.delete();
    indicator.setText(IvyBundle.message("tasks.installIvyDevtool.progress.complete"));
    indicator.setFraction(1);
  }

  @NotNull
  private String getIvyDevtoolDirectory() {
    PreferenceService.Cache cache = preferenceService.getCache();
    return cache.getIvyEngineDirectory()
        + cache.getIvyEngineDefinition().getApplicationDirectory()
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

  public void deployModule(@NotNull IvyModule ivyModule, @NotNull Task nextTask) {
    Path source =
        Paths.get(Preconditions.checkNotNull(ivyModule.getContentRoot().getCanonicalPath()));
    Path target =
        Paths.get(
            preferenceService.getCache().getIvyEngineDirectory()
                + "/system/applications/Portal/"
                + ivyModule.getName()
                + "/1");
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

      ProgressManager.getInstance().run(nextTask);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  @NotNull
  private Optional<String> getIvyEngineUrl() {
    //    List<String> ports = ImmutableList.of("8080", "8081", "8082", "8083", "8084", "8085");
    //    for (String port : ports) {
    //      try {
    //        int statusCode =
    //            Request.Head("http://localhost:" + port + "/ivy/info/index.jsp")
    //                .execute()
    //                .returnResponse()
    //                .getStatusLine()
    //                .getStatusCode();
    //        if (statusCode == 200) {
    //          return Optional.of("http://localhost:" + port);
    //        }
    //      } catch (Exception ex) {
    //        LOG.info(
    //            "Ivy Engine is not running on port: " + port + ", got exception: " +
    // ex.getMessage());
    //      }
    //    }
    //    return Optional.empty();
    return Optional.ofNullable(ivyEngineService.getRuntime().getPort())
        .map(port -> "http://localhost:" + port);
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
}
