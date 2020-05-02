package saigonwithlove.ivy.intellij.toolwindow.engine;

import com.intellij.icons.AllIcons;
import javax.swing.Icon;
import javax.swing.tree.DefaultMutableTreeNode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import saigonwithlove.ivy.intellij.shared.Configuration;

@Getter
@AllArgsConstructor
public class ServerPropertyNode extends DefaultMutableTreeNode implements EngineViewNode<Configuration> {
  private Configuration userObject;

  @Override
  public Icon getIcon() {
    return AllIcons.FileTypes.Config;
  }

  @Override
  public String getLabel() {
    return this.userObject.getName();
  }
}
