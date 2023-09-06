package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.components.BorderLayoutPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saigonwithlove.ivy.intellij.shared.IvyBundle;

public class IvySettingsView implements SearchableConfigurable, Configurable.NoScroll {
  private static final Logger LOG =
      Logger.getInstance("#" + IvySettingsView.class.getCanonicalName());

  private final PreferenceService preferenceService;
  private final JBTextField engineDirectoryField;
  private final JBCheckBox deployIvyModulesWhenIvyEngineStartedCheckBox;

  public IvySettingsView(@NotNull Project project) {
    this.preferenceService = project.getService(PreferenceService.class);
    this.engineDirectoryField = new JBTextField();
    this.deployIvyModulesWhenIvyEngineStartedCheckBox =
        new JBCheckBox(IvyBundle.message("settings.engine.deployAllIvyModulesWhenStartedLabel"));
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
    PreferenceService.State state = preferenceService.getState();
    this.engineDirectoryField.setText(state.getIvyEngineDirectory());
    this.deployIvyModulesWhenIvyEngineStartedCheckBox.setSelected(
        state.isDeployIvyModulesWhenIvyEngineStarted());

    JBPanel wrapper = new JBPanel(new GridBagLayout());
    this.addEngineDirectorySetting(wrapper);
    this.addDeployIvyModulesWhenIvyEngineStartedSetting(wrapper);
    this.addSpacer(wrapper);
    return wrapper;
  }

  @Override
  public boolean isModified() {
    PreferenceService.State state = this.preferenceService.getState();
    return !StringUtils.equals(state.getIvyEngineDirectory(), this.engineDirectoryField.getText())
        || state.isDeployIvyModulesWhenIvyEngineStarted()
            != this.deployIvyModulesWhenIvyEngineStartedCheckBox.isSelected();
  }

  @Override
  public void apply() {
    this.preferenceService.update(
        state -> {
          state.setIvyEngineDirectory(this.engineDirectoryField.getText());
          state.setDeployIvyModulesWhenIvyEngineStarted(
              this.deployIvyModulesWhenIvyEngineStartedCheckBox.isSelected());
          return state;
        });
  }

  @Override
  public void reset() {
    PreferenceService.State state = this.preferenceService.getState();
    this.engineDirectoryField.setText(state.getIvyEngineDirectory());
    this.deployIvyModulesWhenIvyEngineStartedCheckBox.setSelected(
        state.isDeployIvyModulesWhenIvyEngineStarted());
  }

  private void addEngineDirectorySetting(JBPanel<?> wrapper) {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.anchor = GridBagConstraints.FIRST_LINE_START;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx = 0;
    constraints.gridy = 0;
    constraints.weightx = 1.0;

    JBPanel<BorderLayoutPanel> content = new JBPanel<>(new BorderLayout(12, 0));
    JBLabel engineDirectoryLabel =
        new JBLabel(IvyBundle.message("settings.engine.engineDirectoryLabel"));
    content.add(engineDirectoryLabel, BorderLayout.WEST);
    JPanel engineDirectoryPanel =
        GuiUtils.constructFieldWithBrowseButton(
            engineDirectoryField,
            ev -> {
              FileChooserDescriptor descriptor =
                  FileChooserDescriptorFactory.createSingleFolderDescriptor()
                      .withTitle("Select Ivy Engine Directory");
              VirtualFile file =
                  FileChooser.chooseFile(descriptor, engineDirectoryField, null, null);
              if (file != null) {
                engineDirectoryField.setText(FileUtil.toSystemDependentName(file.getPath()));
                engineDirectoryField.postActionEvent();
              }
            });
    engineDirectoryPanel.setPreferredSize(new Dimension(600, 30));
    content.add(engineDirectoryPanel, BorderLayout.CENTER);

    wrapper.add(content, constraints);
  }

  private void addDeployIvyModulesWhenIvyEngineStartedSetting(JBPanel<?> wrapper) {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx = 0;
    constraints.gridy = 1;
    constraints.weightx = 1.0;

    wrapper.add(this.deployIvyModulesWhenIvyEngineStartedCheckBox, constraints);
  }

  private void addSpacer(JBPanel<?> wrapper) {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.gridx = 0;
    constraints.gridy = 2;
    constraints.weighty = 1.0;
    constraints.weightx = 1.0;

    JPanel content = new JPanel(new BorderLayout());
    wrapper.add(content, constraints);
  }
}
