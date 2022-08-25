package saigonwithlove.ivy.intellij.engine;

import com.google.common.base.Preconditions;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.io.PrintWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saigonwithlove.ivy.intellij.devtool.IvyDevtools;
import saigonwithlove.ivy.intellij.shared.Configuration;
import saigonwithlove.ivy.intellij.shared.GeneralRunProfile;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.IvyModule;

public abstract class BaseIvyEngine implements IvyEngine {
  private static final Logger LOG =
      Logger.getInstance("#" + BaseIvyEngine.class.getCanonicalName());

  private static final Pattern PORT_PATTERN = Pattern.compile(".*http://.*:(80[0-9]{2})/(ivy)?.*");
  private static final String READY_TEXT = "Axon.ivy Engine is running and ready to serve";

  @Getter private final String directory;
  @Getter private final ArtifactVersion version;
  @Getter private final IvyEngineDefinition definition;

  @Getter private Status status = Status.STOPPED;
  @Getter private int port = -1;

  @Nullable private RunContentDescriptor runContentDescriptor;
  private final Project project;
  private final Subject<IvyEngine> ivyEngineSubject;

  protected BaseIvyEngine(
      @NotNull String directory,
      @NotNull ArtifactVersion version,
      @NotNull IvyEngineDefinition definition,
      @NotNull Project project) {
    this.directory = directory;
    this.version = version;
    this.definition = definition;
    this.project = project;

    this.ivyEngineSubject = BehaviorSubject.createDefault(this);
  }

  @SneakyThrows
  @NotNull
  @Override
  public Single<IvyEngine> start() {
    if (this.status == Status.RUNNING) {
      return Single.just(this);
    }

    if (this.status == Status.STARTING) {
      return this.ivyEngineSubject
          .filter(item -> item.getStatus() == Status.RUNNING)
          .firstElement()
          .toSingle();
    }

    // Common technique was used in JavaScript to hold the "this" reference.
    IvyEngine self = this;
    GeneralCommandLine commandLine =
        new GeneralCommandLine(this.directory + "/" + this.definition.getStartCommand())
            .withEnvironment("JAVA_HOME", this.getCompatibleJavaHome(definition.getJdkVersion()))
            .withWorkDirectory(this.directory)
            .withParameters("start");
    ProgramRunner.Callback callback =
        descriptor -> {
          ProcessListener ivyEngineProcessListener =
              new ProcessListener() {
                @Override
                public void startNotified(@NotNull ProcessEvent event) {
                  port = -1;
                  status = Status.STARTING;
                  LOG.info("Set Ivy Engine status to " + status + " and port to " + port);
                  ivyEngineSubject.onNext(self);
                }

                @Override
                public void processWillTerminate(
                    @NotNull ProcessEvent event, boolean willBeDestroyed) {
                  port = -1;
                  status = Status.STOPPING;
                  LOG.info("Ivy Engine is " + status);
                  ivyEngineSubject.onNext(self);
                }

                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                  LOG.info("Set Ivy Engine port to -1 when Ivy Engine is STOPPED.");
                  port = -1;
                  status = Status.STOPPED;
                  ivyEngineSubject.onNext(self);
                }

                @Override
                public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                  String text = event.getText();
                  // Set Ivy Engine running port.
                  if (port == -1) {
                    Matcher matcher = PORT_PATTERN.matcher(text);
                    if (matcher.find()) {
                      int newPort = Integer.parseInt(matcher.group(1));
                      port = newPort;
                      LOG.info("Axon.ivy Engine is running on port: " + newPort);
                      ivyEngineSubject.onNext(self);
                    }
                  }

                  // Set Ivy Engine running Status.
                  // Axon.ivy 7 and 8 start with READY_TEXT.
                  // Axon.ivy 6 have indentation or a dot in the end, lead to missing READ_TEXT.
                  // That's why we use contains(...).
                  if (status == Status.STARTING && text.contains(READY_TEXT)) {
                    status = Status.RUNNING;
                    LOG.info("Axon.ivy Engine is " + status);
                    ivyEngineSubject.onNext(self);
                  }
                }
              };
          descriptor.getProcessHandler().addProcessListener(ivyEngineProcessListener);

          // Set the descriptor for interactive command in stop method.
          this.runContentDescriptor = descriptor;
        };
    ExecutionEnvironment environment =
        ExecutionEnvironmentBuilder.create(
                this.project,
                DefaultRunExecutor.getRunExecutorInstance(),
                new GeneralRunProfile(commandLine, IvyBundle.message("tasks.runIvyEngine.title")))
            .executionId(ExecutionEnvironment.getNextUnusedExecutionId())
            .build(callback);
    environment.getRunner().execute(environment);
    return this.ivyEngineSubject
        .filter(item -> item.getStatus() == Status.RUNNING)
        .firstElement()
        .toSingle();
  }

  @NotNull
  @Override
  public Single<IvyEngine> stop() {
    if (this.status == Status.STOPPED) {
      return Single.just(this);
    }

    if (this.status == Status.STOPPING) {
      return this.ivyEngineSubject
          .filter(item -> item.getStatus() == Status.STOPPED)
          .firstElement()
          .toSingle();
    }

    if (Objects.nonNull(this.runContentDescriptor)) {
      try (PrintWriter writer =
          new PrintWriter(this.runContentDescriptor.getProcessHandler().getProcessInput())) {
        writer.println("shutdown");
        this.status = Status.STOPPING;
        this.ivyEngineSubject.onNext(this);
      }
    }

    return this.ivyEngineSubject
        .filter(item -> item.getStatus() == Status.STOPPED)
        .firstElement()
        .toSingle();
  }

  public abstract void initialize();

  @Override
  public void deployIvyModule(@NotNull IvyModule ivyModule) {
    IvyDevtools.deployIvyModule(this, ivyModule);
    Flowable.range(0, 120)
        .delay(1, TimeUnit.SECONDS)
        .map(item -> IvyDevtools.getModuleStatus(this, ivyModule.getName()))
        .takeUntil(
            item -> {
              return "ACTIVE".equals(item);
            })
        .blockingSubscribe(
            item -> LOG.info("Deploying module: " + ivyModule.getName() + ", status: " + item));
  }

  @Override
  public void buildIntellijLibraries() {
    LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable();
    Optional.ofNullable(LocalFileSystem.getInstance().findFileByPath(this.getDirectory()))
        .ifPresent(directory -> directory.refresh(false, true));
    this.getDefinition()
        .getLibraries()
        .forEach(
            ivyLibrary ->
                IvyLibraries.defineLibrary(this.getDirectory(), libraryTable, ivyLibrary));
  }

  @Override
  public void subscribe(@NotNull Observer<IvyEngine> observer) {
    this.ivyEngineSubject.subscribe(observer);
  }

  @Override
  public boolean isIvyModuleDeployed(@NotNull IvyModule ivyModule) {
    Optional<VirtualFile> processModelVersionDirectoryOpt =
        Optional.ofNullable(
            LocalFileSystem.getInstance()
                .refreshAndFindFileByPath(
                    this.getDirectory()
                        + "/"
                        + this.getDefinition().getDefaultApplicationDirectory()
                        + "/"
                        + ivyModule.getName()
                        + "/1"));
    return processModelVersionDirectoryOpt.map(VirtualFile::isDirectory).orElse(Boolean.FALSE);
  }

  @Override
  public boolean isIvyModuleNotDeployed(@NotNull IvyModule ivyModule) {
    return !this.isIvyModuleDeployed(ivyModule);
  }

  @Override
  public void updateGlobalVariable(@NotNull Configuration configuration) {
    IvyDevtools.updateGlobalVariable(this, configuration);
  }

  @Override
  public void updateServerProperty(@NotNull Configuration configuration) {
    IvyDevtools.updateServerProperty(this, configuration);
  }

  @Override
  public Map<String, Configuration> getServerProperties() {
    return IvyDevtools.getServerProperties(this);
  }

  @NotNull
  @Override
  public List<String> getProcessModels() {
    return Optional.ofNullable(
            LocalFileSystem.getInstance()
                .refreshAndFindFileByPath(
                    this.getDirectory()
                        + this.getDefinition()
                            .getDefaultApplicationDirectory())) // get Portal directory
        .map(VirtualFile::getChildren) // get all Process Models as array
        .map(Arrays::stream) // convert to Stream
        .orElse(Stream.empty()) // return empty Stream if directory or Process Model is null
        .filter(child -> child.isDirectory() && !"files".equals(child.getName()))
        .map(VirtualFile::getName) // convert from VirtualFile to String (Process Model name)
        .collect(Collectors.toList());
  }

  @NotNull
  @Override
  public Optional<URL> getUrl() {
    if (this.getPort() == -1) {
      return Optional.empty();
    }
    return Optional.of(newEngineUrl(this.getPort()));
  }

  @SneakyThrows
  @NotNull
  protected URL newEngineUrl(int port) {
    return new URL("http://localhost:" + port);
  }

  @NotNull
  protected String getCompatibleJavaHome(@NotNull JavaSdkVersion jdkVersion) {
    Supplier<RuntimeException> noJdkFoundExceptionSupplier =
        () ->
            new NoSuchElementException(
                MessageFormat.format("Could not find JDK version: {0}", jdkVersion));
    return Arrays.stream(ProjectJdkTable.getInstance().getAllJdks())
        .filter(sdk -> "JavaSDK".equals(sdk.getSdkType().getName()))
        .filter(
            sdk ->
                jdkVersion
                    == JavaSdkVersion.fromVersionString(
                        Preconditions.checkNotNull(sdk.getVersionString())))
        .findFirst()
        .map(Sdk::getHomePath)
        .orElseThrow(noJdkFoundExceptionSupplier);
  }
}
