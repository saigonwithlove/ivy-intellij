package saigonwithlove.ivy.intellij.action;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.components.JBList;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import javax.swing.Icon;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.mirror.FileSyncProcessor;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

public class DeployModuleAction extends AnAction {
  private Project project;
  private PreferenceService preferenceService;
  private IvyEngineService ivyEngineService;
  private JBList<Module> modules;

  public DeployModuleAction(
      @NotNull String text,
      @NotNull String description,
      @NotNull Icon icon,
      @NotNull Project project,
      @NotNull PreferenceService preferenceService,
      @NotNull IvyEngineService ivyEngineService,
      @NotNull JBList<Module> modules) {
    super(text, description, icon);
    this.project = project;
    this.preferenceService = preferenceService;
    this.ivyEngineService = ivyEngineService;
    this.modules = modules;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    Module selectedModule = modules.getSelectedValue();
    // Reload module
    Task reloadModuleTask = newReloadModuleTask(project, selectedModule);
    // Sync code with Ivy Engine
    Task syncModuleTask = newSyncModuleTask(project, selectedModule, reloadModuleTask);
    // Compile Java code
    CompileStatusNotification compiledNotification =
        (aborted, errors, warnings, compileContext) -> {
          ProgressManager.getInstance().run(syncModuleTask);
        };
    CompilerManager.getInstance(project).make(selectedModule, compiledNotification);
  }

  private Task newReloadModuleTask(@NotNull Project project, @NotNull Module module) {
    return new Task.Backgroundable(
        project, IvyBundle.message("tasks.reloadModule.title", module.getName())) {
      @Override
      public void run(@NotNull ProgressIndicator progressIndicator) {
        try {
          progressIndicator.setFraction(0.7);
          progressIndicator.setText(
              IvyBundle.message("tasks.reloadModule.progress.connecting", module.getName()));
          Optional<String> baseIvyEngineUrlOpt = ivyEngineService.getIvyEngineUrl();
          if (baseIvyEngineUrlOpt.isPresent()) {
            URI reloadModuleUri =
                new URIBuilder(
                        baseIvyEngineUrlOpt.get()
                            + "/ivy/pro/Portal/ivy-devtool/16AE38ED14569A2A/engine.ivp")
                    .addParameter("command", "module$reload")
                    .addParameter("pm", module.getName())
                    .addParameter("pmv", "1")
                    .build();
            progressIndicator.setFraction(0.9);
            progressIndicator.setText(
                IvyBundle.message("tasks.reloadModule.progress.reloading", module.getName()));
            Request.Get(reloadModuleUri).execute().returnContent();
          }
        } catch (URISyntaxException ex) {
          throw new IllegalArgumentException(ex);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }
    };
  }

  @NotNull
  private Task newSyncModuleTask(@NotNull Project project, @NotNull Module module, @NotNull Task nextTask) {
    return new Task.Backgroundable(
        project, IvyBundle.message("tasks.syncModule.title", module.getName())) {
      @Override
      public void run(@NotNull ProgressIndicator progressIndicator) {
        Path source = Paths.get(module.getModuleFile().getParent().getCanonicalPath());
        Path target =
            Paths.get(
                preferenceService.getState().getIvyEngineDirectory()
                    + "/system/applications/Portal/"
                    + module.getName()
                    + "/1");
        FileSyncProcessor.Options options = new FileSyncProcessor.Options();
        FileSyncProcessor fileSyncProcessor = new FileSyncProcessor();
        FileSyncProcessor.UserInterface ui =
            new FileSyncProcessor.UserInterface() {

              @Override
              public void writeInfo(String s) {
                double fraction = progressIndicator.getFraction();
                progressIndicator.setFraction(fraction + (1 - fraction) * 0.1);
              }

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
          progressIndicator.setFraction(1.0);
          ProgressManager.getInstance().run(nextTask);
        } catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    };
  }
}
