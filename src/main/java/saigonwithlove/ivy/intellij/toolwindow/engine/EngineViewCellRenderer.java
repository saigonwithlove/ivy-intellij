package saigonwithlove.ivy.intellij.toolwindow.engine;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import javax.swing.JTree;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class EngineViewCellRenderer extends ColoredTreeCellRenderer {
  @Override
  public void customizeCellRenderer(
      @NotNull JTree tree,
      Object value,
      boolean selected,
      boolean expanded,
      boolean leaf,
      int row,
      boolean hasFocus) {
    if (value instanceof GlobalVariableNode) {
      GlobalVariableNode node = (GlobalVariableNode) value;
      setIcon(node.getIcon());
      append(node.getUserObject().getName(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
      append(StringUtils.SPACE);
      append(node.getUserObject().getValue(), SimpleTextAttributes.GRAY_ATTRIBUTES);
    } else {
      EngineViewNode node = (EngineViewNode) value;
      setIcon(node.getIcon());
      append(node.getLabel(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }
  }
}
