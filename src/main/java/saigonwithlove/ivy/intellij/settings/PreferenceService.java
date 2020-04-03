package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saigonwithlove.ivy.intellij.engine.IvyEngineDefinition;
import saigonwithlove.ivy.intellij.shared.IvyModule;
import saigonwithlove.ivy.intellij.shared.GeneralObservable;

@State(name = "ivy")
public class PreferenceService implements PersistentStateComponent<PreferenceService.State> {
  private State state;
  private Cache cache;

  @NotNull
  public State getState() {
    if (state == null) {
      state = new State();
    }
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    this.state = state;
  }

  @NotNull
  public Cache getCache() {
    if (cache == null) {
      cache = Cache.builder().build();
    }
    return cache;
  }

  @Data
  public static class State {
    private String ivyEngineDirectory;
    private Map<String, String> modifiedGlobalVariables;
  }

  @Getter
  @Builder
  public static class Cache {
    private String ivyEngineDirectory;
    private boolean enabled;
    @Builder.Default private IvyDevtoolState ivyDevtool = IvyDevtoolState.builder().build();
    private IvyEngineDefinition ivyEngineDefinition;
    @Builder.Default private List<IvyModule> ivyModules = new ArrayList<>();
    @Builder.Default private IvyEngineState ivyEngine = IvyEngineState.builder().build();

    @Builder.Default private GeneralObservable enabledObservable = new GeneralObservable();
    @Builder.Default private GeneralObservable ivyEngineDefinitionObservable = new GeneralObservable();
    @Builder.Default private GeneralObservable ivyModulesObservable = new GeneralObservable();
    @Builder.Default private GeneralObservable ivyEngineDirectoryObservable = new GeneralObservable();

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
      this.enabledObservable.notifyObservers(enabled);
    }

    public void setIvyEngineDefinition(@NotNull IvyEngineDefinition ivyEngineDefinition) {
      this.ivyEngineDefinition = ivyEngineDefinition;
      this.ivyEngineDefinitionObservable.notifyObservers(ivyEngineDefinition);
    }

    public void setIvyModules(@NotNull List<IvyModule> ivyModules) {
      this.ivyModules = ivyModules;
      this.ivyModulesObservable.notifyObservers(ivyModules);
    }

    public void setIvyEngineDirectory(@Nullable String ivyEngineDirectory) {
      this.ivyEngineDirectory = Optional.ofNullable(ivyEngineDirectory).orElse(StringUtils.EMPTY);
      this.ivyEngineDirectoryObservable.notifyObservers(ivyEngineDirectory);
    }
  }

}
