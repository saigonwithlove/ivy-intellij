package saigonwithlove.ivy.intellij.shared;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

@Getter
@Builder
public class Configuration {
  private String name;
  @Setter private String value;
  private String defaultValue;

  public boolean isModified() {
    return value != null && !StringUtils.equals(value, defaultValue);
  }

  public String getValue() {
    return value != null ? value : defaultValue;
  }
}
