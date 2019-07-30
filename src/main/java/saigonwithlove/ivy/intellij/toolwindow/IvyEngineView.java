package saigonwithlove.ivy.intellij.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.compiler.CompileStatusNotification;
import com.intellij.openapi.compiler.CompilerManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import java.awt.BorderLayout;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.Arrays;
import javax.swing.JComponent;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.utils.URIBuilder;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.mirror.FileSyncProcessor;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.Modules;
import saigonwithlove.ivy.intellij.shared.Projects;

public class IvyEngineView extends JBPanel<IvyEngineView> {
  private static final Logger LOG =
      Logger.getInstance("#" + IvyEngineView.class.getCanonicalName());
  private JBList<Module> modules;

  public IvyEngineView(@NotNull Project project) {
    super(new BorderLayout());
    modules = new JBList<>();
    add(newToolbar(project), BorderLayout.WEST);
    add(newContent(project, modules), BorderLayout.CENTER);
  }

  @NotNull
  private JComponent newContent(Project project, JBList<Module> modules) {
    JBPanel panel = new JBPanel(new BorderLayout());
    CollectionListModel<Module> model = new CollectionListModel<>();
    Arrays.stream(ModuleManager.getInstance(project).getSortedModules())
        .filter(Modules::isIvyModule)
        .sorted(Modules::compareByName)
        .forEach(model::add);
    modules.setModel(model);
    modules.setCellRenderer(new ModuleCellRenderer(Projects.getMavenModels(project)));
    panel.add(modules, BorderLayout.WEST);
    return new JBScrollPane(panel);
  }

  @NotNull
  private JComponent newToolbar(@NotNull Project project) {
    DefaultActionGroup actions = new DefaultActionGroup();
    // Start Engine
    AnAction startEngine =
        new AnAction(
            IvyBundle.message("toolWindow.actions.startEngine.tooltip"),
            IvyBundle.message("toolWindow.actions.startEngine.description"),
            AllIcons.Actions.Execute) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent ex) {
            PreferenceService.State preferences =
                ServiceManager.getService(project, PreferenceService.class).getState();
            ServiceManager.getService(project, IvyEngineService.class)
                .startIvyEngine(preferences.getIvyEngineDirectory(), project);
          }
        };
    actions.add(startEngine);

    // Deploy
    AnAction deploy =
        new AnAction(
            IvyBundle.message("toolWindow.actions.deployModule.tooltip"),
            IvyBundle.message("toolWindow.actions.deployModule.description"),
            AllIcons.Nodes.Deploy) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent ev) {
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
        };
    PreferenceService.State preferences =
        ServiceManager.getService(project, PreferenceService.class).getState();
    if (preferences.isIvyDevToolEnabled()) {
      actions.add(deploy);
    }

    // Setting
    actions.add(new Separator());
    AnAction settings =
        new AnAction(
            IvyBundle.message("toolWindow.actions.settings.tooltip"),
            IvyBundle.message("toolWindow.actions.settings.description"),
            AllIcons.General.Settings) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent ex) {
            ShowSettingsUtil.getInstance()
                .showSettingsDialog(project, IvyBundle.message("settings.displayName"));
          }
        };
    actions.add(settings);

    return ActionManager.getInstance()
        .createActionToolbar("IvyServerToolbar", actions, false)
        .getComponent();
  }

  @NotNull
  private Task newSyncModuleTask(@NotNull Project project, Module module, Task nextTask) {
    return new Task.Backgroundable(
        project, IvyBundle.message("tasks.syncModule.title", module.getName())) {
      @Override
      public void run(@NotNull ProgressIndicator progressIndicator) {
        Path source = Paths.get(module.getModuleFile().getParent().getCanonicalPath());
        Path target =
            Paths.get(
                ServiceManager.getService(project, PreferenceService.class)
                        .getState()
                        .getIvyEngineDirectory()
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

  private Task newReloadModuleTask(@NotNull Project project, Module module) {
    return new Task.Backgroundable(
        project, IvyBundle.message("tasks.reloadModule.title", module.getName())) {
      @Override
      public void run(@NotNull ProgressIndicator progressIndicator) {
        try {
          progressIndicator.setFraction(0.1);
          progressIndicator.setText(
              IvyBundle.message("tasks.reloadModule.progress.connecting", module.getName()));
          URI reloadModuleUri =
              new URIBuilder(
                      "http://127.0.0.1:8081/ivy/pro/Portal/ivy-devtool/16AE38ED14569A2A/engine.ivp")
                  .addParameter("command", "module$reload")
                  .addParameter("pm", module.getName())
                  .addParameter("pmv", "1")
                  .build();
          progressIndicator.setFraction(0.5);
          progressIndicator.setText(
              IvyBundle.message("tasks.reloadModule.progress.reloading", module.getName()));
          Request.Get(reloadModuleUri).execute().returnContent();
        } catch (URISyntaxException ex) {
          throw new IllegalArgumentException(ex);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }
    };
  }
}
