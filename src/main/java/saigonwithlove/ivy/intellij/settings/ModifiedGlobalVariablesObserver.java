package saigonwithlove.ivy.intellij.settings;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public class ModifiedGlobalVariablesObserver implements Observer {
  @NonNull private PreferenceService.State state;

  @Override
  public void update(Observable observable, Object object) {
    Map<String, String> modifiedGlobalVariables = Maps.newHashMap((Map<String, String>) object);
    state.setModifiedGlobalVariables(modifiedGlobalVariables);
  }
}
