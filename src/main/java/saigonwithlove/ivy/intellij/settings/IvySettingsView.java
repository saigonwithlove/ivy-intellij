package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.LibrarySyncInvoker;

import javax.swing.*;
import java.awt.*;

public class IvySettingsView implements SearchableConfigurable, Configurable.NoScroll {
  private Project project;
  private PreferenceService preferenceService;
  private JBTextField engineDirectoryField;

  public IvySettingsView(@NotNull Project project, @NotNull PreferenceService preferenceService) {
    this.project = project;
    this.preferenceService = preferenceService;
    this.engineDirectoryField = new JBTextField();
  }

  @NotNull
  @Override
  public String getId() {
    return IvyBundle.message("settings.displayName");
  }

  @Nls(capitalization = Nls.Capitalization.Title)
  @Override
  public String getDisplayName() {
    return IvyBundle.message("settings.displayName");
  }

  @Nullable
  @Override
  public JComponent createComponent() {
    PreferenceService.State preferences = preferenceService.getState();
    engineDirectoryField.setText(preferences.getIvyEngineDirectory());

    JBPanel wrapper = new JBPanel(new GridBagLayout());
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.FIRST_LINE_START;
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.weighty = 1.0;
    constraints.weightx = 1.0;

    JBPanel content = new JBPanel(new BorderLayout(12, 0));
    JBLabel engineDirectoryLabel =
        new JBLabel(IvyBundle.message("settings.engine.engineDirectoryLabel"));
    content.add(engineDirectoryLabel, BorderLayout.WEST);
    JPanel engineDirectoryPanel =
        GuiUtils.constructDirectoryBrowserField(engineDirectoryField, "ivyEngineDirectory");
    engineDirectoryPanel.setPreferredSize(new Dimension(600, 30));
    content.add(engineDirectoryPanel, BorderLayout.CENTER);

    wrapper.add(content, constraints);
    return wrapper;
  }

  @Override
  public boolean isModified() {
    PreferenceService.State preferences = preferenceService.getState();
    return !StringUtils.equals(preferences.getIvyEngineDirectory(), engineDirectoryField.getText());
  }

  @Override
  public void apply() throws ConfigurationException {
    PreferenceService.State preferences = preferenceService.getState();
    preferences.setIvyEngineDirectory(engineDirectoryField.getText());
    ServiceManager.getService(project, LibrarySyncInvoker.class).syncLibraries(project, preferences);
  }

  @Override
  public void reset() {
    PreferenceService.State preferences = preferenceService.getState();
    engineDirectoryField.setText(preferences.getIvyEngineDirectory());
  }
}
