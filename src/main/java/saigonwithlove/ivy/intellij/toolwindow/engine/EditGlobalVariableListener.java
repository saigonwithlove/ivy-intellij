package saigonwithlove.ivy.intellij.toolwindow.engine;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.TextRange;
import com.intellij.ui.treeStructure.Tree;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Objects;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.devtool.IvyDevtoolService;
import saigonwithlove.ivy.intellij.engine.IvyEngineRuntime;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.Configuration;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

public class EditGlobalVariableListener extends MouseAdapter {
  private Project project;
  private Tree tree;
  private PreferenceService preferenceService;
  private IvyDevtoolService ivyDevtoolService;
  private IvyEngineService ivyEngineService;

  public EditGlobalVariableListener(@NotNull Project project, @NotNull Tree tree) {
    this.project = project;
    this.tree = tree;
    this.preferenceService = ServiceManager.getService(project, PreferenceService.class);
    this.ivyDevtoolService = ServiceManager.getService(project, IvyDevtoolService.class);
    this.ivyEngineService = ServiceManager.getService(project, IvyEngineService.class);
  }

  @Override
  public void mouseClicked(MouseEvent event) {
    if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 2) {
      Object object = tree.getLastSelectedPathComponent();
      if (!(object instanceof GlobalVariableNode)) {
        return;
      }

      GlobalVariableNode node = (GlobalVariableNode) object;
      Configuration configuration = node.getUserObject();
      String newValue =
          Messages.showInputDialog(
              project,
              configuration.getName(),
              IvyBundle.message("toolWindow.engine.globalVariable.editDialog.title"),
              node.getIcon(),
              configuration.getValue(),
              null,
              new TextRange(0, StringUtils.length(configuration.getValue())));
      if (Objects.nonNull(newValue)) {
        preferenceService
            .getCache()
            .getIvyEngine()
            .putModifiedGlobalVariable(configuration.getName(), newValue);
        if (ivyEngineService.getRuntime().getStatus() == IvyEngineRuntime.Status.RUNNING) {
          ivyDevtoolService.updateGlobalVariable(configuration.getName(), newValue);
        }
      }
    }
  }
}
