package saigonwithlove.ivy.intellij.shared;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageDialogBuilder;
import com.intellij.openapi.ui.Messages;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.settings.PreferenceService;

@NoArgsConstructor
public class LibrarySyncInvoker {

  public void syncLibraries(@NotNull Project project, PreferenceService.State preferences) {
    if (StringUtils.isBlank(preferences.getIvyEngineDirectory())) {
      return;
    }
    IvyEngineService engineService = ServiceManager.getService(project, IvyEngineService.class);
    if (engineService.isOsgiFolderExist(preferences.getIvyEngineDirectory())) {
      ApplicationManager.getApplication()
          .runWriteAction(
              () ->
                  ServiceManager.getService(project, IvyEngineService.class)
                      .addLibraries(preferences.getIvyEngineDirectory()));
    } else {
      int result =
          MessageDialogBuilder.yesNo(
                  "Warning",
                  "OSGI folder not exist! You should start engine at least one by click Start "
                      + "Engine or Manually Start by clicking Play button in Ivy Plugin Panel then restart IDE.")
              .yesText("Start Engine")
              .noText("Manually start")
              .project(project)
              .show();
      if (result == Messages.YES) {
        engineService.startIvyEngine(preferences.getIvyEngineDirectory(), project);
      }
    }
  }
}
