package saigonwithlove.ivy.intellij.toolwindow;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.JBColor;
import com.intellij.ui.SideBorder;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Arrays;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.action.DeployModuleAction;
import saigonwithlove.ivy.intellij.action.OpenSettingsAction;
import saigonwithlove.ivy.intellij.action.StartEngineAction;
import saigonwithlove.ivy.intellij.devtool.IvyDevtoolService;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.Modules;

public class ModuleView extends JBPanel<ModuleView> {
  private static final Logger LOG = Logger.getInstance("#" + ModuleView.class.getCanonicalName());

  public ModuleView(@NotNull Project project) {
    super(new BorderLayout());
    JBList<Module> modules = new JBList<>();
    add(newToolbar(project, modules), BorderLayout.WEST);
    add(newContent(project, modules), BorderLayout.CENTER);
  }

  @NotNull
  private JComponent newContent(Project project, JBList<Module> modules) {
    JBPanel panel = new JBPanel(new GridBagLayout());
    CollectionListModel<Module> model = new CollectionListModel<>();
    Arrays.stream(ModuleManager.getInstance(project).getSortedModules())
        .filter(Modules::isIvyModule)
        .sorted(Modules.MODULE_COMPARATOR)
        .forEach(model::add);
    modules.setModel(model);
    modules.setCellRenderer(new ModuleCellRenderer(project));

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.FIRST_LINE_START;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = constraints.weighty = 1.0;
    panel.add(modules, constraints  );

    JBScrollPane scrollPanel = new JBScrollPane(panel);
    scrollPanel.setBorder(new SideBorder(JBColor.border(), SideBorder.LEFT));
    return scrollPanel;
  }

  @NotNull
  private JComponent newToolbar(@NotNull Project project, JBList<Module> modules) {
    PreferenceService preferenceService =
        ServiceManager.getService(project, PreferenceService.class);
    IvyEngineService ivyEngineService = ServiceManager.getService(project, IvyEngineService.class);
    IvyDevtoolService ivyDevtoolService =
        ServiceManager.getService(project, IvyDevtoolService.class);
    DefaultActionGroup actions = new DefaultActionGroup();
    // Start Engine
    actions.add(
        new StartEngineAction(project, preferenceService, ivyEngineService, ivyDevtoolService));

    // Deploy
    actions.add(
        new DeployModuleAction(
            project, preferenceService, ivyDevtoolService, modules));

    // Setting
    actions.add(new Separator());
    actions.add(new OpenSettingsAction(project));

    return ActionManager.getInstance()
        .createActionToolbar("IvyServerToolbar", actions, false)
        .getComponent();
  }
}
