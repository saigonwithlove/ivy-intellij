package saigonwithlove.ivy.intellij.toolwindow.engine;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Separator;
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
import javax.swing.JComponent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.action.OpenSettingsAction;
import saigonwithlove.ivy.intellij.action.StartEngineAction;
import saigonwithlove.ivy.intellij.settings.CacheObserver;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.Configuration;
import saigonwithlove.ivy.intellij.shared.Configurations;
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
    PreferenceService preferenceService = project.getService(PreferenceService.class);

    // Root wrapper for content
    JBPanel panel = new JBPanel(new GridBagLayout());

    // Application tree
    //    MutableTreeNode application = new ApplicationNode("Portal");
    // Global variable tree
    MutableTreeNode globalVariablesRoot = newGlobalVariables(preferenceService);
    // Server property tree
    MutableTreeNode serverPropertyRoot =
        new ServerPropertyRoot(IvyBundle.message("toolWindow.engine.serverProperty.title"));

    // EngineView root node
    MutableTreeNode root = new DefaultMutableTreeNode();
    //    root.insert(application, 0);
    root.insert(globalVariablesRoot, 0);
    root.insert(serverPropertyRoot, 1);
    Tree tree = new Tree(root);
    tree.setRootVisible(false);
    tree.setCellRenderer(new EngineViewCellRenderer());
    tree.addMouseListener(new EditGlobalVariableListener(project, tree));
    tree.addMouseListener(new EditServerPropertyListener(project, tree));

    preferenceService
        .asObservable()
        .map(PreferenceService.State::getGlobalVariables)
        .subscribe(updateGlobalVariablesTree(globalVariablesRoot, tree));

    preferenceService
        .asObservable()
        .map(PreferenceService.State::getServerProperties)
        .subscribe(updateServerPropertiesTree(serverPropertyRoot, tree));

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
  private CacheObserver<Map<String, Configuration>> updateServerPropertiesTree(
      @NotNull MutableTreeNode serverPropertyRoot, @NotNull Tree tree) {
    return new CacheObserver<>(
        "Update Server Properties in Engine View",
        serverProperties -> {
          updateServerProperties(serverPropertyRoot, serverProperties);
          ((DefaultTreeModel) tree.getModel()).reload(serverPropertyRoot);
        });
  }

  @NotNull
  private CacheObserver<Map<String, Configuration>> updateGlobalVariablesTree(
      @NotNull MutableTreeNode globalVariablesRoot, @NotNull Tree tree) {
    return new CacheObserver<>(
        "Update Global Variables in Engine View",
        globalVariables -> {
          updateGlobalVariables(globalVariablesRoot, globalVariables);
          ((DefaultTreeModel) tree.getModel()).reload(globalVariablesRoot);
        });
  }

  @NotNull
  private MutableTreeNode newGlobalVariables(PreferenceService preferenceService) {
    DefaultMutableTreeNode globalVariablesRoot =
        new GlobalVariableRoot(IvyBundle.message("toolWindow.engine.globalVariable.title"));
    this.updateGlobalVariables(
        globalVariablesRoot, preferenceService.getState().getGlobalVariables());
    return globalVariablesRoot;
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

  private void updateGlobalVariables(
      @NotNull MutableTreeNode globalVariablesRoot,
      @NotNull Map<String, Configuration> globalVariables) {
    while (globalVariablesRoot.getChildCount() > 0) {
      globalVariablesRoot.remove(globalVariablesRoot.getChildCount() - 1);
    }
    List<Configuration> items = Configurations.buildConfigurations(globalVariables);
    for (int i = 0; i < items.size(); i++) {
      globalVariablesRoot.insert(new GlobalVariableNode(items.get(i)), i);
    }
  }

  private void updateServerProperties(
      MutableTreeNode serverPropertyRoot, Map<String, Configuration> serverProperties) {
    while (serverPropertyRoot.getChildCount() > 0) {
      serverPropertyRoot.remove(serverPropertyRoot.getChildCount() - 1);
    }
    List<Configuration> items = Configurations.buildConfigurations(serverProperties);
    for (int i = 0; i < items.size(); i++) {
      serverPropertyRoot.insert(new ServerPropertyNode(items.get(i)), i);
    }
  }
}
