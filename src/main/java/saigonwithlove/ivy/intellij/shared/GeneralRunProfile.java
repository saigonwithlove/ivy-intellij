package saigonwithlove.ivy.intellij.shared;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import javax.swing.Icon;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@AllArgsConstructor
public class GeneralRunProfile implements RunProfile {
  @NonNull private GeneralCommandLine commandLine;
  @NonNull private String name;

  @NotNull
  @Override
  public RunProfileState getState(
      @NotNull final Executor executor, @NotNull final ExecutionEnvironment env) {
    return new CommandLineState(env) {
      @Override
      @NotNull
      protected OSProcessHandler startProcess() throws ExecutionException {
        final OSProcessHandler processHandler = new OSProcessHandler.Silent(commandLine);
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
