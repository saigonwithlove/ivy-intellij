package saigonwithlove.ivy.intellij.engine;

import com.google.common.base.Preconditions;
import com.intellij.execution.ExecutionException;
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
import com.intellij.openapi.util.Key;
import java.io.PrintWriter;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.shared.GeneralObservable;
import saigonwithlove.ivy.intellij.shared.GeneralRunProfile;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

@Getter
@RequiredArgsConstructor
public class IvyEngineRuntime {
  private static final Logger LOG =
      Logger.getInstance("#" + IvyEngineRuntime.class.getCanonicalName());

  @NonNull private Project project;
  @NonNull private String ivyEngineDirectory;
  @NonNull private IvyEngineDefinition ivyEngineDefinition;
  private String port;
  private Status status = Status.CREATED;
  private RunContentDescriptor runContentDescriptor;

  private GeneralObservable observable = new GeneralObservable();

  public void start() {
    if (this.status != Status.CREATED) {
      LOG.info("Could not start Ivy Engine runtime with status: " + this.status);
      return;
    }

    this.setStatus(Status.STARTING);
    String ivyCommand = ivyEngineDefinition.getStartCommand();
    GeneralCommandLine commandLine =
        new GeneralCommandLine(ivyEngineDirectory + ivyCommand)
            .withEnvironment("JAVA_HOME", getCompatipleJavaHome())
            .withWorkDirectory(ivyEngineDirectory)
            .withParameters("start");
    try {
      ExecutionEnvironment environment =
          ExecutionEnvironmentBuilder.create(
                  project,
                  DefaultRunExecutor.getRunExecutorInstance(),
                  new GeneralRunProfile(commandLine, IvyBundle.message("tasks.runIvyEngine.title")))
              .executionId(ExecutionEnvironment.getNextUnusedExecutionId())
              .build();
      environment.getRunner().execute(environment, new IvyEngineExecutionCallback(this));
    } catch (ExecutionException ex) {
      LOG.error(ex);
    }
  }

  private void setStatus(@NotNull Status status) {
    this.status = status;
    this.observable.notifyObservers(this);
  }

  public void stop() {
    if (Objects.nonNull(this.runContentDescriptor)) {
      try (PrintWriter writer =
          new PrintWriter(this.runContentDescriptor.getProcessHandler().getProcessInput())) {
        writer.println("shutdown");
      }
    }
  }

  @NotNull
  private String getCompatipleJavaHome() {
    JavaSdkVersion requiredJdkVersion = ivyEngineDefinition.getJdkVersion();
    Supplier<RuntimeException> noJdkFoundExceptionSupplier =
        () ->
            new NoSuchElementException(
                MessageFormat.format("Could not find JDK version: {0}", requiredJdkVersion));
    return Arrays.stream(ProjectJdkTable.getInstance().getAllJdks())
        .filter(sdk -> "JavaSDK".equals(sdk.getSdkType().getName()))
        .filter(
            sdk ->
                requiredJdkVersion
                    == JavaSdkVersion.fromVersionString(
                        Preconditions.checkNotNull(sdk.getVersionString())))
        .findFirst()
        .map(Sdk::getHomePath)
        .orElseThrow(noJdkFoundExceptionSupplier);
  }

  public static enum Status {
    CREATED,
    STARTING,
    RUNNING,
    STOPPING,
    STOPPED;
  }

  @AllArgsConstructor
  public static class IvyEngineExecutionCallback implements ProgramRunner.Callback {
    @NonNull private IvyEngineRuntime runtime;

    @Override
    public void processStarted(RunContentDescriptor descriptor) {
      this.runtime.runContentDescriptor = descriptor;
      descriptor.getProcessHandler().addProcessListener(new IvyEngineProcessListener(runtime));
    }
  }

  @AllArgsConstructor
  public static class IvyEngineProcessListener implements ProcessListener {
    private static final Pattern PORT_PATTERN = Pattern.compile(".*http://.*:([0-9]{4})/ivy.*");
    private static final String READY_TEXT = "Axon.ivy Engine is running and ready to serve";

    @NonNull private IvyEngineRuntime runtime;

    @Override
    public void startNotified(@NotNull ProcessEvent event) {}

    @Override
    public void processTerminated(@NotNull ProcessEvent event) {
      this.runtime.setStatus(Status.STOPPED);
    }

    @Override
    public void processWillTerminate(@NotNull ProcessEvent event, boolean willBeDestroyed) {
      this.runtime.setStatus(Status.STOPPING);
    }

    @Override
    public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
      if (Objects.isNull(this.runtime.port)) {
        Matcher matcher = PORT_PATTERN.matcher(event.getText());
        if (matcher.find()) {
          this.runtime.port = matcher.group(1);
          LOG.info("Axon.ivy Engine is running on port: " + this.runtime.port);
        }
      }

      if (this.runtime.status == Status.STARTING && event.getText().startsWith(READY_TEXT)) {
        this.runtime.setStatus(Status.RUNNING);
      }
    }
  }
}
