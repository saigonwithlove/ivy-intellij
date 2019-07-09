package saigonwithlove.ivy.intellij.toolwindow;

import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.components.JBPanel;
import java.awt.BorderLayout;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.settings.PreferenceService;

public class IvyEngineView extends JBPanel<IvyEngineView> {

  public IvyEngineView(@NotNull Project project) {
    super(new BorderLayout());
    add(newToolbar(project), BorderLayout.WEST);
    ConsoleView engineConsole = newIvyEngineConsole(project);
    add(engineConsole.getComponent(), BorderLayout.CENTER);
  }

  @NotNull
  private com.intellij.execution.ui.ConsoleView newIvyEngineConsole(@NotNull Project project) {
    com.intellij.execution.ui.ConsoleView console = new ConsoleViewImpl(project, true);
    Disposer.register(project, console);
    return console;
  }

  @NotNull
  private JComponent newToolbar(@NotNull Project project) {
    DefaultActionGroup actions = new DefaultActionGroup();
    // Start Engine
    AnAction startEngine =
        new AnAction("Start Ivy Engine", null, AllIcons.Actions.Execute) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent ex) {
            PreferenceService.State preferences =
                ServiceManager.getService(project, PreferenceService.class).getState();
            ServiceManager.getService(project, IvyEngineService.class)
                .startIvyEngine(preferences.getIvyEngineDirectory(), project);
          }
        };
    actions.add(startEngine);

    // Setting
    actions.add(new Separator());
    AnAction settings =
        new AnAction("Open Settings Dialog", null, AllIcons.General.Settings) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent ex) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, "Ivy");
          }
        };
    actions.add(settings);

    return ActionManager.getInstance()
        .createActionToolbar("IvyServerToolbar", actions, false)
        .getComponent();
  }
}
