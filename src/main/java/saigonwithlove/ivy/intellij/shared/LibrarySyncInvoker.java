package saigonwithlove.ivy.intellij.shared;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;
import saigonwithlove.ivy.intellij.settings.PreferenceService;

@NoArgsConstructor
public class LibrarySyncInvoker {
    private static final Logger LOG = Logger.getInstance("#" + LibrarySyncInvoker.class.getCanonicalName());

    public void syncLibraries(@NotNull Project project, PreferenceService.State preferences) {
        if(StringUtils.isBlank(preferences.getIvyEngineDirectory())) {
            return;
        }
        IvyEngineService engineService = ServiceManager.getService(project, IvyEngineService.class);
        if(IvyEngine.isEngine(preferences.getIvyEngineDirectory())) {
            if (!IvyEngine.isOsgiFolderExist(preferences.getIvyEngineDirectory())) {
                ApplicationManager.getApplication().invokeLater(
                        ()->{
                            IvyEngine.cleanUpEngineLog(preferences.getIvyEngineDirectory());
                            engineService.startIvyEngine(preferences.getIvyEngineDirectory(), project);
                            if(IvyEngine.engineUpAndRun(preferences.getIvyEngineDirectory())) {
                                ApplicationManager.getApplication().runWriteAction(() -> {
                                    engineService.addLibraries(preferences.getIvyEngineDirectory());
                                });
                            };
                        },
                        ModalityState.NON_MODAL);
            } else {
                ApplicationManager.getApplication().runWriteAction(() -> {
                    engineService.addLibraries(preferences.getIvyEngineDirectory());
                });
            }
        }
    }
}
