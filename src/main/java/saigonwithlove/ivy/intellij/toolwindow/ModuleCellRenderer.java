package saigonwithlove.ivy.intellij.toolwindow;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import java.util.List;
import javax.swing.JList;
import lombok.AllArgsConstructor;
import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.shared.Modules;

@AllArgsConstructor
public class ModuleCellRenderer extends ColoredListCellRenderer<Module> {
  private static final Logger LOG =
      Logger.getInstance("#" + ModuleCellRenderer.class.getCanonicalName());

  private List<Model> models;

  @Override
  protected void customizeCellRenderer(
      @NotNull JList list, Module value, int index, boolean selected, boolean hasFocus) {
    setIcon(AllIcons.Nodes.Module);

    SimpleTextAttributes textAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
    List<Dependency> missingDependencies = Modules.getMissingIvyDependencies(value, models);
    if (CollectionUtils.isNotEmpty(missingDependencies)) {
      textAttributes = SimpleTextAttributes.ERROR_ATTRIBUTES;
      setToolTipText(
          "Missing "
              + missingDependencies.stream()
                  .map(dependency -> dependency.getArtifactId() + "-" + dependency.getVersion())
                  .reduce((a, b) -> a + ", " + b)
                  .orElse(""));
    }

    append(value.getName(), textAttributes);

    Modules.toMavenModel(value)
        .ifPresent(
            model -> {
              append("  " + model.getVersion(), SimpleTextAttributes.GRAY_ATTRIBUTES);
            });
  }
}
