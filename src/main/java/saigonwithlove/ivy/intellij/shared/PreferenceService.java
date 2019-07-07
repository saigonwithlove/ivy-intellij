package saigonwithlove.ivy.intellij.shared;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.util.xmlb.annotations.Property;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

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
    @Property private String ivyEngineDirectory;
  }
}
