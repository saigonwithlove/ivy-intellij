package saigonwithlove.ivy.intellij.toolwindow.engine;

import com.intellij.ui.ColoredTreeCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import javax.swing.JTree;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

public class EngineViewCellRenderer extends ColoredTreeCellRenderer {
  private static final SimpleTextAttributes MODIFIED_ATTRIBUTES =
      new SimpleTextAttributes(
          SimpleTextAttributes.STYLE_PLAIN,
          new JBColor(101 << 16 | 205 << 8 | 196, 139 << 16 | 233 << 8 | 253));

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
      append(
          node.getUserObject().getName(),
          node.getUserObject().isModified()
              ? MODIFIED_ATTRIBUTES
              : SimpleTextAttributes.REGULAR_ATTRIBUTES);
      append(StringUtils.SPACE);
      append(
          node.getUserObject().getValue(),
          node.getUserObject().isModified()
              ? SimpleTextAttributes.REGULAR_ATTRIBUTES
              : SimpleTextAttributes.GRAYED_ATTRIBUTES);
    } else if (value instanceof ServerPropertyNode) {
      ServerPropertyNode node = (ServerPropertyNode) value;
      setIcon(node.getIcon());
      append(
          node.getUserObject().getName(),
          node.getUserObject().isModified()
              ? MODIFIED_ATTRIBUTES
              : SimpleTextAttributes.REGULAR_ATTRIBUTES);
      append(StringUtils.SPACE);
      append(
          node.getUserObject().getValue(),
          node.getUserObject().isModified()
              ? SimpleTextAttributes.REGULAR_ATTRIBUTES
              : SimpleTextAttributes.GRAYED_ATTRIBUTES);
    } else if (value instanceof EngineViewNode) {
      EngineViewNode<?> node = (EngineViewNode<?>) value;
      setIcon(node.getIcon());
      append(node.getLabel(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    }
  }
}
