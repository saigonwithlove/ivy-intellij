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
import saigonwithlove.ivy.intellij.engine.IvyEngine;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.Configuration;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

public class EditServerPropertyListener extends MouseAdapter {
  private Project project;
  private Tree tree;
  private PreferenceService preferenceService;
  private IvyDevtoolService ivyDevtoolService;

  public EditServerPropertyListener(@NotNull Project project, @NotNull Tree tree) {
    this.project = project;
    this.tree = tree;
    this.preferenceService = ServiceManager.getService(project, PreferenceService.class);
    this.ivyDevtoolService = ServiceManager.getService(project, IvyDevtoolService.class);
  }

  @Override
  public void mouseClicked(MouseEvent event) {
    if (event.getButton() == MouseEvent.BUTTON1 && event.getClickCount() == 2) {
      Object object = tree.getLastSelectedPathComponent();
      if (!(object instanceof ServerPropertyNode)) {
        return;
      }

      ServerPropertyNode node = (ServerPropertyNode) object;
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
        preferenceService.update(
            state -> {
              configuration.setValue(newValue);
              state.getServerProperties().put(configuration.getName(), configuration);
              return state;
            });
        IvyEngine engine = preferenceService.getState().getIvyEngine();
        if (engine != null && engine.getStatus() == IvyEngine.Status.RUNNING) {
          ivyDevtoolService.updateServerProperty(configuration.getName(), newValue);
        }
      }
    }
  }
}
