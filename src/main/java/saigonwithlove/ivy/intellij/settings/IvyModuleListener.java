package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.ModuleListener;
import com.intellij.openapi.project.Project;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.shared.IvyModule;
import saigonwithlove.ivy.intellij.shared.Modules;

public class IvyModuleListener implements ModuleListener {
  @Override
  public void moduleRemoved(@NotNull Project project, @NotNull Module module) {
    PreferenceService preferenceService = project.getService(PreferenceService.class);
    List<IvyModule> initialModulesCopy =
        new ArrayList<>(preferenceService.getState().getIvyModules());
    List<IvyModule> remainingModules =
        initialModulesCopy.stream()
            .filter(item -> !item.getName().equalsIgnoreCase(module.getName()))
            .collect(Collectors.toList());
    preferenceService.update(
        (state -> {
          state.setIvyModules(remainingModules);
          return state;
        }));
  }

  @Override
  public void modulesAdded(@NotNull Project project, @NotNull List<Module> modules) {
    for (Module module : modules) {
      Optional<IvyModule> importModule = Modules.toIvyModule(module);
      if (importModule.isPresent()) {
        PreferenceService preferenceService = project.getService(PreferenceService.class);
        List<IvyModule> allModules = new ArrayList<>(preferenceService.getState().getIvyModules());
        allModules.add(importModule.get());
        preferenceService.update(
            (state -> {
              state.setIvyModules(allModules);
              return state;
            }));
      }
    }
  }
}
