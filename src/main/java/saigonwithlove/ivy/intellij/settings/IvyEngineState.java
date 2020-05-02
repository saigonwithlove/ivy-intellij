package saigonwithlove.ivy.intellij.settings;

import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
@Builder
@ToString
public class IvyEngineState {
  @Builder.Default private Map<String, String> globalVariables = Collections.emptyMap();
  @Builder.Default private Map<String, String> modifiedGlobalVariables = Collections.emptyMap();
  @Builder.Default private Map<String, String> serverProperties = Collections.emptyMap();
  @Builder.Default private Map<String, String> modifiedServerProperties = Collections.emptyMap();

  public void putModifiedGlobalVariable(@NotNull String name, @NotNull String value) {
    Map<String, String> variables = new HashMap<>(this.modifiedGlobalVariables);
    variables.put(name, value);
    this.modifiedGlobalVariables = ImmutableMap.copyOf(variables);
  }

  public void putModifiedServerProperties(@NotNull String name, @NotNull String value) {
    Map<String, String> properties = new HashMap<>(this.modifiedServerProperties);
    properties.put(name, value);
    this.modifiedServerProperties = ImmutableMap.copyOf(properties);
  }
}
