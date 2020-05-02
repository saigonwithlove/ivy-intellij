package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Consumer;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngineDefinition;
import saigonwithlove.ivy.intellij.shared.GeneralObservable;
import saigonwithlove.ivy.intellij.shared.IvyModule;

@State(name = "ivy")
public class PreferenceService implements PersistentStateComponent<PreferenceService.State> {
  private State state;
  private Cache cache;
  private GeneralObservable observable;

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

  @NotNull
  private Observable getObservable() {
    if (observable == null) {
      observable = new GeneralObservable();
    }
    return observable;
  }

  public void addObserver(@NotNull Observer observer) {
    getObservable().addObserver(observer);
  }

  public void update(Consumer<Cache> updater) {
    updater.accept(getCache());
    observable.notifyObservers(getCache());
  }

  @Data
  public static class State {
    private String ivyEngineDirectory;
    private Map<String, String> modifiedGlobalVariables;
    private Map<String, String> modifiedServerProperties;
  }

  @Getter
  @Setter
  @Builder
  public static class Cache {
    private String ivyEngineDirectory;
    private boolean enabled;
    @Builder.Default private IvyDevtoolState ivyDevtool = IvyDevtoolState.builder().build();
    private IvyEngineDefinition ivyEngineDefinition;
    @Builder.Default private List<IvyModule> ivyModules = new ArrayList<>();
    @Builder.Default private IvyEngineState ivyEngine = IvyEngineState.builder().build();
  }
}
