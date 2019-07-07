package saigonwithlove.ivy.intellij.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.Optional;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.PreferenceService;

public class EngineSettingsDialog extends DialogWrapper {
  private JBTextField engineDirectoryField;

  public EngineSettingsDialog(@NotNull Project project, @NotNull PreferenceService.State preferences) {
    super(project);
    engineDirectoryField = new JBTextField(preferences.getIvyEngineDirectory());
    init();
    setResizable(false);
    setTitle(IvyBundle.message("toolWindow.engine.settingsDialog.title"));
  }

  @Override
  protected JComponent createCenterPanel() {
    JBPanel content = new JBPanel(new BorderLayout());
    content.setPreferredSize(new Dimension(600, -1));
    JBLabel engineDirectoryLabel =
        new JBLabel(IvyBundle.message("toolWindow.engine.settingsDialog.engineDirectory"));
    content.add(engineDirectoryLabel, BorderLayout.WEST);
    JPanel engineDirectoryPanel =
        GuiUtils.constructDirectoryBrowserField(engineDirectoryField, "engine directory");
    content.add(engineDirectoryPanel, BorderLayout.CENTER);
    return content;
  }

  @NotNull
  public String getEngineDirectory() {
    return Optional.ofNullable(engineDirectoryField.getText())
        .filter(StringUtils::isNotBlank)
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "The engine path should not be null! The engine directory was not selected or Ok button was not clicked."));
  }
}
