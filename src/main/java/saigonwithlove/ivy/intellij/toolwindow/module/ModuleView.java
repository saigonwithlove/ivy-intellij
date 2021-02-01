package saigonwithlove.ivy.intellij.toolwindow.module;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
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
import java.util.List;
import javax.swing.JComponent;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.action.DeployModuleAction;
import saigonwithlove.ivy.intellij.settings.CacheObserver;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.IvyModule;

public class ModuleView extends JBPanel<ModuleView> {
  private static final Logger LOG = Logger.getInstance("#" + ModuleView.class.getCanonicalName());

  public ModuleView(@NotNull Project project) {
    super(new BorderLayout());
    JBList<IvyModule> modules = new JBList<>();
    add(newToolbar(project, modules), BorderLayout.WEST);
    add(newContent(project, modules), BorderLayout.CENTER);
  }

  @NotNull
  private JComponent newContent(@NotNull Project project, @NotNull JBList<IvyModule> modules) {
    JBPanel panel = new JBPanel(new GridBagLayout());
    PreferenceService preferenceService =
        ServiceManager.getService(project, PreferenceService.class);
    PreferenceService.State state = preferenceService.getState();
    CollectionListModel<IvyModule> model = new CollectionListModel<>(state.getIvyModules());
    preferenceService
        .asObservable()
        .map(PreferenceService.State::getIvyModules)
        .subscribe(createModelUpdater(model));
    modules.setModel(model);
    modules.setCellRenderer(new ModuleCellRenderer(project));

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.FIRST_LINE_START;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = constraints.weighty = 1.0;
    panel.add(modules, constraints);

    JBScrollPane scrollPanel = new JBScrollPane(panel);
    scrollPanel.setBorder(new SideBorder(JBColor.border(), SideBorder.LEFT));
    return scrollPanel;
  }

  private CacheObserver<List<IvyModule>> createModelUpdater(CollectionListModel<IvyModule> model) {
    return new CacheObserver<>(
        "Update Ivy Modules in Module View",
        ivyModules -> {
          model.removeAll();
          model.add(ivyModules);
        });
  }

  @NotNull
  private JComponent newToolbar(@NotNull Project project, @NotNull JBList<IvyModule> modules) {
    DefaultActionGroup actions = new DefaultActionGroup();

    // Deploy
    actions.add(new DeployModuleAction(project, modules));

    return ActionManager.getInstance()
        .createActionToolbar("ModuleViewToolbar", actions, false)
        .getComponent();
  }
}
