package saigonwithlove.ivy.intellij.settings;

import com.google.common.collect.Maps;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import saigonwithlove.ivy.intellij.shared.GeneralObservable;

@Getter
@Builder
@ToString
public class IvyEngineState {
  @Builder.Default private Map<String, String> globalVariables = new HashMap<>();
  @Builder.Default private Map<String, String> defaultGlobalVariables = new HashMap<>();
  @Builder.Default private Map<String, String> modifiedGlobalVariables = new HashMap<>();
  @Builder.Default private Map<String, String> systemProperties = new HashMap<>();

  @Builder.Default private GeneralObservable globalVariablesObservable = new GeneralObservable();
  @Builder.Default private GeneralObservable systemPropertiesObservable = new GeneralObservable();

  @Builder.Default
  private GeneralObservable modifiedGlobalVariablesObservable = new GeneralObservable();

  public void setModifiedGlobalVariables(@Nullable Map<String, String> modifiedGlobalVariables) {
    this.modifiedGlobalVariables =
        Optional.ofNullable(modifiedGlobalVariables).orElseGet(HashMap::new);
    this.globalVariables =
        this.syncGlobalVariables(this.defaultGlobalVariables, this.modifiedGlobalVariables);
    this.getModifiedGlobalVariablesObservable().notifyObservers(this.modifiedGlobalVariables);
    this.globalVariablesObservable.notifyObservers(this.globalVariables);
  }

  public void setDefaultGlobalVariables(@NotNull Map<String, String> defaultGlobalVariables) {
    this.defaultGlobalVariables = defaultGlobalVariables;
    this.globalVariables =
        this.syncGlobalVariables(this.defaultGlobalVariables, this.modifiedGlobalVariables);
    this.globalVariablesObservable.notifyObservers(this.globalVariables);
  }

  public void putModifiedGlobalVariable(@NotNull String name, @NotNull String value) {
    this.modifiedGlobalVariables.put(name, value);
    this.globalVariables =
        this.syncGlobalVariables(this.defaultGlobalVariables, this.modifiedGlobalVariables);
    this.getModifiedGlobalVariablesObservable().notifyObservers(this.modifiedGlobalVariables);
    this.globalVariablesObservable.notifyObservers(this.globalVariables);
  }

  public void setSystemProperties(@NotNull Map<String, String> systemProperties) {
    this.systemProperties = systemProperties;
    this.systemPropertiesObservable.notifyObservers(this.systemProperties);
  }

  @NotNull
  private Map<String, String> syncGlobalVariables(
      Map<String, String> defaultGlobalVariables, Map<String, String> modifiedGlobalVariables) {
    // Remove not existed
    List<String> removedVariableNames =
        modifiedGlobalVariables.keySet().stream()
            .filter(name -> !defaultGlobalVariables.containsKey(name))
            .collect(Collectors.toList());
    removedVariableNames.forEach(modifiedGlobalVariables::remove);
    List<String> duplicatedVariableNames =
        modifiedGlobalVariables.keySet().stream()
            .filter(
                name ->
                    StringUtils.equals(
                        modifiedGlobalVariables.get(name), defaultGlobalVariables.get(name)))
            .collect(Collectors.toList());
    duplicatedVariableNames.forEach(modifiedGlobalVariables::remove);
    // Use default variables then override by modified one
    Map<String, String> synchronizedGlobalVariables = Maps.newHashMap(defaultGlobalVariables);
    synchronizedGlobalVariables.putAll(modifiedGlobalVariables);
    return synchronizedGlobalVariables;
  }
}
