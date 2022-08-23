package saigonwithlove.ivy.intellij.action;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngine;
import saigonwithlove.ivy.intellij.settings.PreferenceService;
import saigonwithlove.ivy.intellij.shared.Configuration;
import saigonwithlove.ivy.intellij.shared.IvyBundle;
import saigonwithlove.ivy.intellij.shared.Notifier;

public class UpdateGlobalVariablesAndServerPropertiesAction extends AnAction {
  private static final Logger LOG =
      Logger.getInstance(
          "#" + UpdateGlobalVariablesAndServerPropertiesAction.class.getCanonicalName());

  private Project project;
  private PreferenceService preferenceService;

  public UpdateGlobalVariablesAndServerPropertiesAction(@NotNull Project project) {
    super(
        IvyBundle.message("toolWindow.actions.updateGlobalVariablesAndSystemProperties.tooltip"),
        IvyBundle.message(
            "toolWindow.actions.updateGlobalVariablesAndSystemProperties.description"),
        AllIcons.Actions.Refresh);
    this.project = project;
    this.preferenceService = project.getService(PreferenceService.class);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    ProgressManager.getInstance()
        .run(
            new Task.Backgroundable(
                this.project,
                IvyBundle.message(
                    "toolWindow.actions.updateGlobalVariablesAndSystemProperties.description")) {
              @Override
              public void run(@NotNull ProgressIndicator progressIndicator) {
                progressIndicator.setIndeterminate(true);

                IvyEngine ivyEngine =
                    UpdateGlobalVariablesAndServerPropertiesAction.this
                        .preferenceService
                        .getState()
                        .getIvyEngine();
                if (Objects.isNull(ivyEngine)
                    || ivyEngine.getStatus() != IvyEngine.Status.RUNNING) {
                  Notifier.info(project, IvyBundle.message("notification.ivyEngineStopped"));
                  return;
                }

                LOG.info("Update global variables.");
                Map<String, Configuration> globalVariables =
                    preferenceService.getState().getGlobalVariables();
                globalVariables.values().stream()
                    .filter(Configuration::isModified)
                    .forEach(ivyEngine::updateGlobalVariable);

                LOG.info("Update server properties.");
                Map<String, Configuration> serverProperties =
                    preferenceService.getState().getServerProperties();
                serverProperties.values().stream()
                    .filter(Configuration::isModified)
                    .forEach(ivyEngine::updateServerProperty);
              }
            });
  }
}
