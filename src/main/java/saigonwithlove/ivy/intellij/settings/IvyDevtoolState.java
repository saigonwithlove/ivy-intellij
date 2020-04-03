package saigonwithlove.ivy.intellij.settings;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class IvyDevtoolState {
  private boolean enabled;
}
