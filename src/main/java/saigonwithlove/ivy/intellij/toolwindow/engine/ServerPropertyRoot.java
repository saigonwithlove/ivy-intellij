package saigonwithlove.ivy.intellij.toolwindow.engine;

import com.intellij.icons.AllIcons;
import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ServerPropertyRoot extends DefaultMutableTreeNode implements EngineViewNode<String> {
  private String userObject;

  @Override
  public Icon getIcon() {
    return AllIcons.Nodes.ConfigFolder;
  }

  @Override
  public String getLabel() {
    return this.userObject;
  }
}
