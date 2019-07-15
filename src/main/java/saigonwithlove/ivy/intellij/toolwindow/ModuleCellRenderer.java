package saigonwithlove.ivy.intellij.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.ui.ColoredListCellRenderer;
import javax.swing.JList;
import org.jetbrains.annotations.NotNull;

public class ModuleCellRenderer extends ColoredListCellRenderer<Module> {
  @Override
  protected void customizeCellRenderer(
      @NotNull JList list, Module value, int index, boolean selected, boolean hasFocus) {
    setIcon(AllIcons.Nodes.Module);
    append(value.getName());
  }
}
