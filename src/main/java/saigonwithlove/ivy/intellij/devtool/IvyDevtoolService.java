package saigonwithlove.ivy.intellij.devtool;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

public class IvyDevtoolService {
  private static final Logger LOG =
      Logger.getInstance("#" + IvyDevtoolService.class.getCanonicalName());
  private static final String IVY_DEVTOOL_PACKAGE_URL =
      "https://github.com/saigonwithlove/ivy-devtool/releases/download/v0.2.0/ivy-devtool-0.2.0.iar";
  private static final String IVY_DEVTOOL_URL =
      "/ivy/pro/Portal/ivy-devtool/16AE38ED14569A2A/engine.ivp";

  private Project project;
  private PreferenceService preferenceService;

  public IvyDevtoolService(Project project) {
    this.project = project;
    this.preferenceService = ServiceManager.getService(project, PreferenceService.class);
  }

  public boolean exists() {
    return new File(getIvyDevtoolDirectory()).isDirectory();
  }

  public void install(ProgressIndicator indicator) {
    indicator.setText(IvyBundle.message("tasks.installIvyDevtool.progress.downloading"));
    indicator.setFraction(0.1);
    File ivyDevtoolPackage = downloadIvyDevtool(IVY_DEVTOOL_PACKAGE_URL);
    indicator.setText(IvyBundle.message("tasks.installIvyDevtool.progress.installing"));
    indicator.setFraction(0.5);
    String ivyDevtoolDirectoryText =
        preferenceService.getState().getIvyEngineDirectory()
            + preferenceService.getState().getIvyEngineDefinition().getApplicationDirectory()
            + "/Portal/ivy-devtool/1";
    extract(ivyDevtoolPackage, ivyDevtoolDirectoryText);
    ivyDevtoolPackage.delete();
    indicator.setText(IvyBundle.message("tasks.installIvyDevtool.progress.complete"));
    indicator.setFraction(1);
  }

  @NotNull
  private String getIvyDevtoolDirectory() {
    PreferenceService.State preferences = preferenceService.getState();
    return preferences.getIvyEngineDirectory()
        + preferences.getIvyEngineDefinition().getApplicationDirectory()
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

  public void reloadModule(@NotNull String baseIvyEngineUrl, @NotNull Module module) {
    try {
      URI reloadModuleUri =
          new URIBuilder(baseIvyEngineUrl + IVY_DEVTOOL_URL)
              .addParameter("command", "module$reload")
              .addParameter("pm", module.getName())
              .addParameter("pmv", "1")
              .build();

      Request.Get(reloadModuleUri).execute().returnContent();
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }
}
