package saigonwithlove.ivy.intellij.toolwindow.module;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import java.util.List;
import javax.swing.JList;
import org.apache.commons.collections.CollectionUtils;
import org.apache.maven.model.Dependency;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.IvyModule;
import saigonwithlove.ivy.intellij.shared.Modules;

public class ModuleCellRenderer extends ColoredListCellRenderer<IvyModule> {
  private static final Logger LOG =
      Logger.getInstance("#" + ModuleCellRenderer.class.getCanonicalName());

  private Project project;
  private PreferenceService preferenceService;

  public ModuleCellRenderer(@NotNull Project project) {
    this.project = project;
    this.preferenceService = project.getService(PreferenceService.class);
  }

  @Override
  protected void customizeCellRenderer(
      @NotNull JList list, IvyModule value, int index, boolean selected, boolean hasFocus) {
    setIcon(AllIcons.Nodes.Module);

    SimpleTextAttributes textAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
    List<IvyModule> ivyModules = preferenceService.getState().getIvyModules();
    List<Dependency> missingDependencies = Modules.getMissingIvyDependencies(value, ivyModules);
    if (CollectionUtils.isNotEmpty(missingDependencies)) {
      textAttributes = SimpleTextAttributes.ERROR_ATTRIBUTES;
      String dependenciesText =
          missingDependencies.stream()
              .map(dependency -> dependency.getArtifactId() + "-" + dependency.getVersion())
              .reduce((a, b) -> a + ", " + b)
              .orElse("");
      setToolTipText(IvyBundle.message("toolWindow.module.missingDependencies", dependenciesText));
    }

    append(value.getName(), textAttributes);

    append(" " + value.getMavenModel().getVersion(), SimpleTextAttributes.GRAY_ATTRIBUTES);
  }
}
