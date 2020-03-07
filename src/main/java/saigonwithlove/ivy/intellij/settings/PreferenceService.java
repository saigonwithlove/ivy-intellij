package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.util.xmlb.annotations.Property;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import saigonwithlove.ivy.intellij.engine.IvyEngineDefinition;

@State(name = "ivy")
public class PreferenceService implements PersistentStateComponent<PreferenceService.State> {
  private State state;

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

  @Data
  public static class State {
    private boolean enabled;
    @Property private String ivyEngineDirectory;
    private boolean ivyDevToolEnabled;
    private IvyEngineDefinition ivyEngineDefinition;
  }
}
