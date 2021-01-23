package saigonwithlove.ivy.intellij.engine;

import com.google.common.base.Preconditions;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
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
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.ReplaySubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.io.PrintWriter;
import java.net.URL;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saigonwithlove.ivy.intellij.shared.GeneralRunProfile;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

public class Ivy7Engine implements IvyEngine {
  private static final Logger LOG = Logger.getInstance("#" + Ivy7Engine.class.getCanonicalName());
  private static final Pattern PORT_PATTERN = Pattern.compile(".*http://.*:([0-9]{4})/ivy.*");
  private static final String READY_TEXT = "Axon.ivy Engine is running and ready to serve";

  @Getter @NonNull private final String directory;
  @Getter @NonNull private final ArtifactVersion version;
  @Getter @NonNull private final IvyEngineDefinition definition;
  @NonNull private final Project project;

  @Getter private Status status = Status.STOPPED;
  @Getter private int port = -1;

  @Nullable private RunContentDescriptor runContentDescriptor;

  private final Subject<Status> statusSubject = PublishSubject.create();
  private final Subject<Integer> portSubject = PublishSubject.create();
  private final Subject<IvyEngine> ivyEngineSubject = ReplaySubject.create(1);

  @Builder
  public Ivy7Engine(
      String directory, ArtifactVersion version, IvyEngineDefinition definition, Project project) {
    this.directory = directory;
    this.version = version;
    this.definition = definition;
    this.project = project;

    this.statusSubject.subscribe(
        newStatus -> {
          this.status = newStatus;
          this.ivyEngineSubject.onNext(this);
        });
    this.portSubject.subscribe(
        newPort -> {
          this.port = newPort;
          this.ivyEngineSubject.onNext(this);
        });
  }

  @SneakyThrows
  @Override
  public void start() {
    if (this.status == Status.RUNNING || this.status == Status.STARTING) {
      return;
    }

    this.status = Status.STARTING;

    GeneralCommandLine commandLine =
        new GeneralCommandLine(directory + definition.getStartCommand())
            .withEnvironment("JAVA_HOME", getCompatibleJavaHome(definition.getJdkVersion()))
            .withWorkDirectory(directory)
            .withParameters("start");
    ExecutionEnvironment environment =
        ExecutionEnvironmentBuilder.create(
                project,
                DefaultRunExecutor.getRunExecutorInstance(),
                new GeneralRunProfile(commandLine, IvyBundle.message("tasks.runIvyEngine.title")))
            .executionId(ExecutionEnvironment.getNextUnusedExecutionId())
            .build();

    environment.setCallback(
        descriptor -> {
          ProcessListener portListener =
              new ProcessListener() {
                @Override
                public void startNotified(@NotNull ProcessEvent event) {}

                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                  portSubject.onNext(-1);
                }

                @Override
                public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                  Matcher matcher = PORT_PATTERN.matcher(event.getText());
                  if (matcher.find()) {
                    Integer newPort = Integer.valueOf(matcher.group(1));
                    LOG.info("Axon.ivy Engine is running on port: " + newPort);
                    portSubject.onNext(newPort);
                  }
                }
              };
          descriptor.getProcessHandler().addProcessListener(portListener);

          ProcessListener statusListener =
              new ProcessListener() {
                @Override
                public void startNotified(@NotNull ProcessEvent event) {
                  statusSubject.onNext(Status.STARTING);
                }

                @Override
                public void processTerminated(@NotNull ProcessEvent event) {
                  statusSubject.onNext(Status.STOPPED);
                }

                @Override
                public void processWillTerminate(
                    @NotNull ProcessEvent event, boolean willBeDestroyed) {
                  statusSubject.onNext(Status.STOPPING);
                }

                @Override
                public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
                  if (event.getText().startsWith(READY_TEXT)) {
                    statusSubject.onNext(Status.RUNNING);
                  }
                }
              };
          descriptor.getProcessHandler().addProcessListener(statusListener);

          // Set the descriptor for interactive command in stop method.
          this.runContentDescriptor = descriptor;
        });
    environment.getRunner().execute(environment);
  }

  @Override
  public void stop() {
    if (Objects.nonNull(this.runContentDescriptor)) {
      try (PrintWriter writer =
          new PrintWriter(this.runContentDescriptor.getProcessHandler().getProcessInput())) {
        writer.println("shutdown");
      }
    }
  }

  @SneakyThrows
  @Override
  public Optional<URL> getUrl() {
    if (this.port == -1) {
      return Optional.empty();
    }
    return Optional.of(newEngineUrl(this.port));
  }

  @Override
  public void subscribe(Observer<IvyEngine> observer) {
    this.ivyEngineSubject.subscribe(observer);
  }

  @NotNull
  private String getCompatibleJavaHome(@NotNull JavaSdkVersion jdkVersion) {
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

  @SneakyThrows
  @NotNull
  private URL newEngineUrl(int port) {
    return new URL("http://localhost:" + port);
  }

  public void buildIntellijLibraries() {
    LibraryTable libraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable();
    Optional.ofNullable(LocalFileSystem.getInstance().findFileByPath(this.directory))
        .ifPresent(directory -> directory.refresh(false, true));
    this.definition
        .getLibraries()
        .forEach(
            ivyLibrary -> IvyLibraries.defineLibrary(this.directory, libraryTable, ivyLibrary));
  }
}
