package saigonwithlove.ivy.intellij.engine;

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ExecutionEnvironmentBuilder;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saigonwithlove.ivy.intellij.devtool.IvyDevtools;
import saigonwithlove.ivy.intellij.shared.GeneralRunProfile;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

public class Ivy7Engine implements IvyEngine {
  private static final Logger LOG = Logger.getInstance("#" + Ivy7Engine.class.getCanonicalName());
  private static final Pattern PORT_PATTERN = Pattern.compile(".*http://.*:([0-9]{4})/ivy.*");
  private static final String READY_TEXT = "Axon.ivy Engine is running and ready to serve";

  @Getter @NonNull private final String directory;
  @Getter @NonNull private final ArtifactVersion version;
  @Getter @NonNull private final IvyEngineDefinition definition;

  @Getter @NonNull private Status status = Status.STOPPED;
  @Getter private int port = -1;

  @Nullable private RunContentDescriptor runContentDescriptor;
  @NonNull private final Project project;
  @NonNull private final Subject<IvyEngine> ivyEngineSubject;

  @Builder
  public Ivy7Engine(
      String directory, ArtifactVersion version, IvyEngineDefinition definition, Project project) {
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

    // Common technique was used in JavaScript to hold the "this" reference.
    IvyEngine self = this;
    environment.setCallback(
        descriptor -> {
          ProcessListener ivyEngineProcessListener =
              new ProcessListener() {
                @Override
                public void startNotified(@NotNull ProcessEvent event) {
                  LOG.info("Set Ivy Engine status to STARTING.");
                  port = -1;
                  status = Status.STARTING;
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
                  Matcher matcher = PORT_PATTERN.matcher(event.getText());
                  if (matcher.find()) {
                    int newPort = Integer.parseInt(matcher.group(1));
                    port = newPort;
                    ivyEngineSubject.onNext(self);
                    LOG.info("Axon.ivy Engine is running on port: " + newPort);
                  }
                  if (event.getText().startsWith(READY_TEXT)) {
                    status = Status.RUNNING;
                    ivyEngineSubject.onNext(self);
                    LOG.info("Axon.ivy Engine is " + status);
                  }
                }
              };
          descriptor.getProcessHandler().addProcessListener(ivyEngineProcessListener);

          // Set the descriptor for interactive command in stop method.
          this.runContentDescriptor = descriptor;
        });
    environment.getRunner().execute(environment);
    return this.ivyEngineSubject
        .filter(item -> item.getStatus() == Status.RUNNING)
        .firstElement()
        .toSingle();
  }

  @NotNull
  @Override
  public Observable<IvyEngine> stop() {
    if (this.status == Status.STOPPING || this.status == Status.STOPPED) {
      return this.ivyEngineSubject;
    } else if (Objects.nonNull(this.runContentDescriptor)) {
      try (PrintWriter writer =
          new PrintWriter(this.runContentDescriptor.getProcessHandler().getProcessInput())) {
        writer.println("shutdown");
        this.status = Status.STOPPING;
        this.ivyEngineSubject.onNext(this);
      }
    }
    return this.ivyEngineSubject;
  }

  @Override
  public void subscribe(@NotNull Observer<IvyEngine> observer) {
    this.ivyEngineSubject.subscribe(observer);
  }

  @Override
  public String getDefaultApplicationDirectory() {
    return this.directory + this.definition.getApplicationDirectory() + "/Portal";
  }

  @Override
  public void initialize() {
    // Clean up old Process Models
    this.getProcessModels().stream()
        .filter(
            processModel -> !Arrays.asList("JsfWorkflowUi", "ivy-devtool").contains(processModel))
        .forEach(
            processModel -> {
              try {
                FileUtils.deleteDirectory(
                    new File(
                        this.directory
                            + this.definition.getApplicationDirectory()
                            + "/Portal/"
                            + processModel));
              } catch (IOException ex) {
                LOG.error("Could not delete Process Model: " + processModel, ex);
              }
            });

    IvyDevtools.installIvyDevtool(this);
  }
}
