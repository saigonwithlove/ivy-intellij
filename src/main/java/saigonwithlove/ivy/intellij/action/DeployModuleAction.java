package saigonwithlove.ivy.intellij.action;

import com.intellij.icons.AllIcons;
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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.devtool.IvyDevtoolService;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.mirror.FileSyncProcessor;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.Notifier;

public class DeployModuleAction extends AnAction {
  private Project project;
  private PreferenceService preferenceService;
  private IvyEngineService ivyEngineService;
  private IvyDevtoolService ivyDevtoolService;
  private JBList<Module> modules;

  public DeployModuleAction(
      @NotNull Project project,
      @NotNull PreferenceService preferenceService,
      @NotNull IvyEngineService ivyEngineService,
      @NotNull IvyDevtoolService ivyDevtoolService,
      @NotNull JBList<Module> modules) {
    super(
        IvyBundle.message("toolWindow.actions.deployModule.tooltip"),
        IvyBundle.message("toolWindow.actions.deployModule.description"),
        AllIcons.Nodes.Deploy);
    this.project = project;
    this.preferenceService = preferenceService;
    this.ivyEngineService = ivyEngineService;
    this.ivyDevtoolService = ivyDevtoolService;
    this.modules = modules;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    // Show notification if ivy devtool is disabled
    if (!preferenceService.getState().isIvyDevToolEnabled()) {
      Notifier.info(
          project,
          new OpenUrlAction(
              IvyBundle.message("notification.ivyDevtoolUrlText"),
              "https://github.com/saigonwithlove/ivy-devtool/releases"),
          IvyBundle.message("notification.ivyDevtoolDisabled"));
      return;
    }

    Module selectedModule = modules.getSelectedValue();
    if (Objects.isNull(selectedModule)) {
      Notifier.info(project, IvyBundle.message("notification.deployModuleNotSelected"));
    }

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
        progressIndicator.setFraction(0.7);
        progressIndicator.setText(
            IvyBundle.message("tasks.reloadModule.progress.connecting", module.getName()));
        Optional<String> baseIvyEngineUrlOpt = ivyEngineService.getIvyEngineUrl();
        baseIvyEngineUrlOpt.ifPresent(url -> ivyDevtoolService.reloadModule(url, module));
        progressIndicator.setFraction(1);
        progressIndicator.setText(
            IvyBundle.message("tasks.reloadModule.progress.reloading", module.getName()));
      }
    };
  }

  @NotNull
  private Task newSyncModuleTask(
      @NotNull Project project, @NotNull Module module, @NotNull Task nextTask) {
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
