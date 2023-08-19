package saigonwithlove.ivy.intellij.devtool;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor
public class ModuleProcessModelVersionStatusResponse {
  private String processModel;
  private String processModelVersion;
  private String status;
}
