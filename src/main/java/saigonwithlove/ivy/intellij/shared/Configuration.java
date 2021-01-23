package saigonwithlove.ivy.intellij.shared;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Configuration {
  private String name;
  private String value;
  private String defaultValue;
  private String description;

  public boolean isModified() {
    return value != null && !StringUtils.equals(value, defaultValue);
  }

  public String getValue() {
    return value != null ? value : defaultValue;
  }
}
