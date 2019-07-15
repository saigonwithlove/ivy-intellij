package saigonwithlove.ivy.intellij.shared;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.ModuleRunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import javax.swing.Icon;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class GeneralRunProfile implements ModuleRunProfile {
  private GeneralCommandLine commandLine;
  private String name;

  @Nullable
  @Override
  public RunProfileState getState(
      @NotNull final Executor executor, @NotNull final ExecutionEnvironment env) {
    if (commandLine == null) {
      // can return null if creation of cmd line has been cancelled
      return null;
    }

    return new CommandLineState(env) {
      GeneralCommandLine createCommandLine() {
        return commandLine;
      }

      @Override
      @NotNull
      protected OSProcessHandler startProcess() throws ExecutionException {
        final GeneralCommandLine commandLine = createCommandLine();
        final OSProcessHandler processHandler = new ColoredProcessHandler(commandLine);
        ProcessTerminatedListener.attach(processHandler);
        return processHandler;
      }
    };
  }

  @NotNull
  @Override
  public String getName() {
    return name;
  }

  @Nullable
  @Override
  public Icon getIcon() {
    return null;
  }
}
