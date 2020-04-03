package saigonwithlove.ivy.intellij.toolwindow.engine;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.JBColor;
import com.intellij.ui.SideBorder;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.Tree;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.List;
import java.util.Map;
import java.util.Observer;
import java.util.stream.Collectors;
import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.action.OpenSettingsAction;
import saigonwithlove.ivy.intellij.action.StartEngineAction;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.Configuration;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

public class EngineView extends JBPanel<EngineView> {
  private static final Logger LOG = Logger.getInstance("#" + EngineView.class.getCanonicalName());

  public EngineView(@NotNull Project project) {
    super(new BorderLayout());
    add(newToolbar(project), BorderLayout.WEST);
    add(newContent(project), BorderLayout.CENTER);
  }

  @NotNull
  private JComponent newContent(@NotNull Project project) {
    PreferenceService preferenceService =
        ServiceManager.getService(project, PreferenceService.class);
    PreferenceService.Cache cache = preferenceService.getCache();

    // Root wrapper for content
    JBPanel panel = new JBPanel(new GridBagLayout());

    // Application tree
    //    MutableTreeNode application = new ApplicationNode("Portal");
    // Global variable tree
    MutableTreeNode globalVariables =
        new GlobalVariableRoot(IvyBundle.message("toolWindow.engine.globalVariable.title"));
    // System property tree
    //    MutableTreeNode systemProperties =
    //        new GlobalVariableRoot(IvyBundle.message("toolWindow.engine.systemProperty.title"));

    // EngineView root node
    MutableTreeNode root = new DefaultMutableTreeNode();
    //    root.insert(application, 0);
    root.insert(globalVariables, 0);
    //    root.insert(systemProperties, 2);
    Tree tree = new Tree(root);
    tree.setRootVisible(false);
    tree.setCellRenderer(new EngineViewCellRenderer());
    tree.addMouseListener(new EditGlobalVariableListener(project, tree));

    Observer globalVariablesObserver =
        (observable, object) -> {
          List<Configuration> items =
              ((Map<String, String>) object)
                  .entrySet().stream()
                      .map(entry -> new Configuration(entry.getKey(), entry.getValue()))
                      .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                      .collect(Collectors.toList());
          for (int i = 0; i < globalVariables.getChildCount(); i++) {
            globalVariables.remove(i);
          }
          for (int i = 0; i < items.size(); i++) {
            globalVariables.insert(new GlobalVariableNode(items.get(i)), i);
          }
          ((DefaultTreeModel) tree.getModel()).reload(globalVariables);
        };
    cache.getIvyEngine().getGlobalVariablesObservable().addObserver(globalVariablesObserver);

    GridBagConstraints constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.FIRST_LINE_START;
    constraints.fill = GridBagConstraints.BOTH;
    constraints.weightx = constraints.weighty = 1.0;
    panel.add(new TreeSpeedSearch(tree).getComponent(), constraints);

    JBScrollPane scrollPanel = new JBScrollPane(panel);
    scrollPanel.setBorder(new SideBorder(JBColor.border(), SideBorder.LEFT));
    return scrollPanel;
  }

  @NotNull
  private JComponent newToolbar(@NotNull Project project) {
    DefaultActionGroup actions = new DefaultActionGroup();

    // Start Engine
    actions.add(new StartEngineAction(project));

    // Setting
    actions.add(new Separator());
    actions.add(new OpenSettingsAction(project));

    return ActionManager.getInstance()
        .createActionToolbar("EngineViewToolbar", actions, false)
        .getComponent();
  }
}
