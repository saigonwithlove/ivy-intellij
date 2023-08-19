package saigonwithlove.ivy.intellij.devtool;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import groovy.lang.Newify;
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
import java.text.MessageFormat;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import lombok.experimental.UtilityClass;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Model;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngine;
import saigonwithlove.ivy.intellij.mirror.FileSyncProcessor;
import saigonwithlove.ivy.intellij.shared.Configuration;
import saigonwithlove.ivy.intellij.shared.IvyModule;
import saigonwithlove.ivy.intellij.shared.Modules;

@UtilityClass
public class IvyDevtools {
  public static final String IVY7_DEVTOOL_URL =
      "/ivy/pro/Portal/ivy-devtool/16AE38ED14569A2A/engine.ivp";
  public static final String IVY8_DEVTOOL_URL =
      "/ivy/pro/demo-portal/ivy-devtool/16AE38ED14569A2A/engine.ivp";

  private static final Logger LOG = Logger.getInstance("#" + IvyDevtools.class.getCanonicalName());
  private static final String IVY_DEVTOOL_VERSION_RANGE = "[0.2.3,)";

  public static final String IVY_DEVTOOL_PACKAGE_URL =
      "https://github.com/saigonwithlove/ivy-devtool/releases/download/v0.2.4-SNAPSHOT/ivy-devtool-0.2.4-SNAPSHOT.iar";

  public static void installIvyDevtool(@NotNull IvyEngine ivyEngine) {
    if (ivyDevtoolExists(ivyEngine) && isIvyDevtoolUpdated(ivyEngine)) {
      return;
    }

    deployIarUrl(ivyEngine, IVY_DEVTOOL_PACKAGE_URL);
  }

  public static void deployIvyModule(@NotNull IvyEngine ivyEngine, @NotNull IvyModule ivyModule) {
    Preconditions.checkArgument(
        ivyDevtoolExists(ivyEngine),
        "Could not deploy Ivy Module "
            + ivyModule.getName()
            + " because Ivy Devtool is not existed.");
    Preconditions.checkArgument(
        isIvyDevtoolUpdated(ivyEngine),
        "Could not deploy Ivy Module " + ivyModule.getName() + " because Ivy Devtool is outdated.");

    /*
     * Synchronize Ivy Module from project to Ivy Application.
     */
    Path source =
        Paths.get(Preconditions.checkNotNull(ivyModule.getContentRoot().getCanonicalPath()));
    Path target =
        Paths.get(
            ivyEngine.getDirectory()
                + "/"
                + ivyEngine.getDefinition().getDefaultApplicationDirectory()
                + "/"
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
      reloadIvyModule(ivyEngine, ivyModule);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  private static void reloadIvyModule(@NotNull IvyEngine ivyEngine, @NotNull IvyModule ivyModule) {
    reloadProcessModel(ivyEngine, ivyModule.getName());
  }

  public static void reloadProcessModel(
      @NotNull IvyEngine ivyEngine, @NotNull String processModelName) {
    if (ivyEngine.getStatus() != IvyEngine.Status.RUNNING) {
      LOG.info(
          MessageFormat.format(
              "Ivy Engine is {0}, skip reload Ivy Module {1}",
              ivyEngine.getStatus(), processModelName));
      return;
    }

    try {
      String baseIvyEngineUrl =
          ivyEngine
              .getUrl()
              .map(String::valueOf)
              .orElseThrow(() -> new NoSuchElementException("Could not get URL of Ivy Engine."));
      URI reloadModuleUri =
          new URIBuilder(baseIvyEngineUrl + getIvyDevtoolUrl(ivyEngine))
              .addParameter("command", "module$reload")
              .addParameter("pm", processModelName)
              .addParameter("pmv", "1")
              .build();
      HttpClient client = HttpClientBuilder.create().build();
      int responseStatus =
          client.execute(new HttpGet(reloadModuleUri)).getStatusLine().getStatusCode();
      if (responseStatus != 200) {
        throw new RuntimeException(
            MessageFormat.format(
                "Could not reload module: {0}. Server return error.", processModelName));
      }
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void deployIarUrl(@NotNull IvyEngine ivyEngine, @NotNull String iarUrlText) {
    File iarPackage = downloadIar(iarUrlText);
    Model mavenModel = getMavenModel(iarPackage);
    String pmvDirectoryText =
        ivyEngine.getDirectory()
            + "/"
            + ivyEngine.getDefinition().getDefaultApplicationDirectory()
            + "/"
            + mavenModel.getArtifactId()
            + "/1";
    extractIar(iarPackage, pmvDirectoryText);
    iarPackage.delete();
    reloadProcessModel(ivyEngine, mavenModel.getArtifactId());
  }

  public static void updateServerProperty(
      @NotNull IvyEngine ivyEngine, @NotNull Configuration configuration) {
    // TODO every command involve Ivy Devtool on Ivy Engine should be delegated to IvyDevtools.
    if (ivyEngine.getStatus() != IvyEngine.Status.RUNNING) {
      LOG.info(
          MessageFormat.format(
              "Ivy Engine is {0}, skip updating server property.", ivyEngine.getStatus()));
      return;
    }

    try {
      String baseIvyEngineUrl =
          ivyEngine
              .getUrl()
              .map(String::valueOf)
              .orElseThrow(() -> new NoSuchElementException("Could not get URL of Ivy Engine."));
      URI setServerPropertyUri =
          new URIBuilder(baseIvyEngineUrl + getIvyDevtoolUrl(ivyEngine))
              .addParameter("command", "server-property$set")
              .addParameter("name", configuration.getName())
              .addParameter("value", configuration.getValue())
              .build();
      HttpClient client = HttpClientBuilder.create().build();
      int responseStatus =
          client.execute(new HttpGet(setServerPropertyUri)).getStatusLine().getStatusCode();
      if (responseStatus != 200) {
        throw new RuntimeException(
            MessageFormat.format(
                "Could not set Server Property: {0}. Server return error.",
                configuration.getName()));
      }
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public static void updateGlobalVariable(
      @NotNull IvyEngine ivyEngine, @NotNull Configuration configuration) {
    if (ivyEngine.getStatus() != IvyEngine.Status.RUNNING) {
      LOG.info(
          MessageFormat.format(
              "Ivy Engine is {0}, skip updating global variable.", ivyEngine.getStatus()));
      return;
    }

    try {
      String baseIvyEngineUrl =
          ivyEngine
              .getUrl()
              .map(String::valueOf)
              .orElseThrow(() -> new NoSuchElementException("Could not get URL of Ivy Engine."));
      URI setGlobalVariableUri =
          new URIBuilder(baseIvyEngineUrl + getIvyDevtoolUrl(ivyEngine))
              .addParameter("command", "global-variable$set")
              .addParameter("name", configuration.getName())
              .addParameter("value", configuration.getValue())
              .build();
      HttpClient client = HttpClientBuilder.create().build();
      int responseStatus =
          client.execute(new HttpGet(setGlobalVariableUri)).getStatusLine().getStatusCode();
      if (responseStatus != 200) {
        throw new RuntimeException(
            MessageFormat.format(
                "Could not set Global Variable: {0}. Server return error.",
                configuration.getName()));
      }
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  public Map<String, Configuration> getServerProperties(@NotNull IvyEngine ivyEngine) {
    try {
      String baseIvyEngineUrl =
          ivyEngine
              .getUrl()
              .map(String::valueOf)
              .orElseThrow(() -> new NoSuchElementException("Could not get URL of Ivy Engine."));
      URI getServerPropertiesUri =
          new URIBuilder(baseIvyEngineUrl + getIvyDevtoolUrl(ivyEngine))
              .addParameter("command", "server-property$get-all")
              .build();

      HttpClient client = HttpClientBuilder.create().build();
      HttpResponse response = client.execute(new HttpGet(getServerPropertiesUri));
      int responseStatus = response.getStatusLine().getStatusCode();
      if (responseStatus != 200) {
        throw new RuntimeException("Could not get Server Properties. Server return error.");
      }
      Map<String, String> rawServerProperties =
          (Map<String, String>)
              new ObjectMapper().readValue(response.getEntity().getContent(), Map.class);
      return rawServerProperties.entrySet().stream()
          .collect(
              Collectors.toMap(
                  Map.Entry::getKey,
                  entry ->
                      Configuration.builder()
                          .name(entry.getKey())
                          .defaultValue(entry.getValue())
                          .build()));
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    }
  }

  @NotNull
  public static File downloadIar(@NotNull String iarUrlText) {
    try {
      URL iarUrl = new URL(iarUrlText);
      String fileName = FilenameUtils.getName(iarUrl.getPath());
      String extension = FilenameUtils.getExtension(iarUrl.getPath());
      Path downloadedIarPackage = Files.createTempFile(fileName, "." + extension);
      FileUtils.copyURLToFile(iarUrl, downloadedIarPackage.toFile());
      return downloadedIarPackage.toFile();
    } catch (IOException ex) {
      throw new RuntimeException("Could not download IAR package.", ex);
    }
  }

  public @NotNull static String getModuleStatus(@NotNull IvyEngine ivyEngine, @NotNull String processModelName) {
    try {
      String baseIvyEngineUrl =
          ivyEngine
              .getUrl()
              .map(String::valueOf)
              .orElseThrow(() -> new NoSuchElementException("Could not get URL of Ivy Engine."));
      URI getModuleStatusUri =
          new URIBuilder(baseIvyEngineUrl + getIvyDevtoolUrl(ivyEngine))
              .addParameter("command", "module$status")
              .addParameter("pm", processModelName)
              .addParameter("pmv", "1")
              .build();

      HttpClient client = HttpClientBuilder.create().build();
      HttpResponse response = client.execute(new HttpGet(getModuleStatusUri));
      int responseStatus = response.getStatusLine().getStatusCode();
      if (responseStatus != 200) {
        throw new RuntimeException("Could not get Module status. Server return error.");
      }
      ModuleProcessModelVersionStatusResponse moduleStatus = new ObjectMapper().readValue(response.getEntity().getContent(), ModuleProcessModelVersionStatusResponse.class);
      return moduleStatus.getStatus();
    } catch (URISyntaxException ex) {
      throw new IllegalArgumentException(ex);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @NotNull
  private static File extractIar(
      @NotNull File iarPackage, @NotNull String destinationDirectoryText) {
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(iarPackage))) {
      File ivyDevtoolDirectory = new File(destinationDirectoryText);
      FileUtils.forceMkdir(ivyDevtoolDirectory);
      byte[] buffer = new byte[1024];
      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        String fileName = zipEntry.getName();
        File newFile = new File(destinationDirectoryText + "/" + fileName);
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
      return ivyDevtoolDirectory;
    } catch (IOException ex) {
      throw new RuntimeException("Could not extract IAR package.", ex);
    }
  }

  @NotNull
  private static Model getMavenModel(File iarPackage) {
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(iarPackage))) {
      String pomFileName = "pom.xml";
      Path tmpPom = Files.createTempFile(iarPackage.getName(), ".pom.xml");
      byte[] buffer = new byte[1024];
      ZipEntry zipEntry = zis.getNextEntry();
      while (zipEntry != null) {
        if (pomFileName.equals(zipEntry.getName())) {
          try (FileOutputStream fos = new FileOutputStream(tmpPom.toFile())) {
            int len;
            while ((len = zis.read(buffer)) > 0) {
              fos.write(buffer, 0, len);
            }
          }
          Model mavenModel =
              Optional.ofNullable(
                      LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tmpPom.toFile()))
                  .flatMap(Modules::toMavenModel)
                  .orElseThrow(
                      () ->
                          new NoSuchElementException(
                              "Could not read Maven Model from pom.xml of IAR package."));
          tmpPom.toFile().delete();
          return mavenModel;
        }
        zipEntry = zis.getNextEntry();
      }
      throw new NoSuchElementException("Could not find pom.xml in IAR package.");
    } catch (IOException ex) {
      throw new RuntimeException("Could not extract version from IAR package.", ex);
    }
  }

  private static boolean ivyDevtoolExists(@NotNull IvyEngine ivyEngine) {
    Optional<VirtualFile> ivyDevtoolDirectoryOpt =
        Optional.ofNullable(
            LocalFileSystem.getInstance().refreshAndFindFileByPath(getIvyDevtoolPath(ivyEngine)));
    return ivyDevtoolDirectoryOpt.map(VirtualFile::exists).orElse(Boolean.FALSE);
  }

  @NotNull
  private static String getIvyDevtoolPath(@NotNull IvyEngine ivyEngine) {
    return ivyEngine.getDirectory()
        + "/"
        + ivyEngine.getDefinition().getDefaultApplicationDirectory()
        + "/"
        + ivyEngine.getDefinition().getIvyDevtoolProcessModelVersionPath();
  }

  private static boolean isIvyDevtoolUpdated(@NotNull IvyEngine ivyEngine) {
    VirtualFile ivyDevtoolProcessModelVersion =
        Preconditions.checkNotNull(
            LocalFileSystem.getInstance().refreshAndFindFileByPath(getIvyDevtoolPath(ivyEngine)),
            "Ivy Devtool Process Model Version is not existed.");
    if (ivyDevtoolProcessModelVersion.isDirectory()) {
      return Optional.ofNullable(ivyDevtoolProcessModelVersion.findChild("pom.xml"))
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
    } else {
      Model mavenModel = getMavenModel(new File(getIvyDevtoolPath(ivyEngine)));
      return Optional.of(mavenModel)
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
  }

  private static String getIvyDevtoolUrl(@NotNull IvyEngine ivyEngine) {
    if (ivyEngine.getVersion().getMajorVersion() == 7) {
      return IVY7_DEVTOOL_URL;
    } else if (ivyEngine.getVersion().getMajorVersion() == 8) {
      return IVY8_DEVTOOL_URL;
    } else {
      throw new NoSuchElementException(
          "No Ivy Devtool Url for Ivy Engine version: " + ivyEngine.getVersion());
    }
  }
}
