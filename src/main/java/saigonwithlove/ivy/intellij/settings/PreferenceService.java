package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.util.xmlb.annotations.Transient;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.subjects.BehaviorSubject;
import io.reactivex.rxjava3.subjects.Subject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saigonwithlove.ivy.intellij.engine.IvyEngine;
import saigonwithlove.ivy.intellij.shared.Configuration;
import saigonwithlove.ivy.intellij.shared.IvyModule;

@State(name = "ivy", storages = @Storage("ivy.xml"))
public class PreferenceService implements PersistentStateComponent<PreferenceService.State> {
  private static final Logger LOG =
      Logger.getInstance("#" + PreferenceService.class.getCanonicalName());

  private State state;
  private final Subject<State> stateSubject;

  public PreferenceService() {
    this.state = new State();
    this.stateSubject = BehaviorSubject.createDefault(state);
  }

  @NotNull
  @Override
  public State getState() {
    if (state == null) {
      state = new State();
    }
    return state;
  }

  @Override
  public void loadState(@NotNull State state) {
    this.update((currentState) -> state);
  }

  public Observable<State> asObservable() {
    return this.stateSubject;
  }

  public void update(Function<State, State> stateUpdater) {
    this.state = stateUpdater.apply(this.state);
    this.stateSubject.onNext(this.state);
  }

  @Setter
  @Getter
  @NoArgsConstructor
  public static class State {
    private boolean pluginEnabled;
    private String ivyEngineDirectory;
    private boolean deployIvyModulesWhenIvyEngineStarted;
    private Map<String, Configuration> globalVariables = new HashMap<>();
    private Map<String, Configuration> serverProperties = new HashMap<>();
    private Map<String, List<String>> ivyLibraries = new HashMap<>();

    @Nullable
    @Getter(onMethod = @__({@Transient}))
    private IvyEngine ivyEngine;

    @Getter(onMethod = @__({@Transient}))
    private List<IvyModule> ivyModules = new ArrayList<>();
  }
}
