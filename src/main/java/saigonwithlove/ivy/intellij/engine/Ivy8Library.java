package saigonwithlove.ivy.intellij.engine;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Ivy8Library implements IvyLibrary {
  IVY_CONTAINER(
      "IVY_CONTAINER",
      ImmutableList.of("system/plugins/**/*.jar"),
      ImmutableList.of(
          "system/plugins/ch.ivyteam.ivy.webservice.configuration_*.jar",
          "system/plugins/ch.ivyteam.ivy.webservice.exec.cxf_*.jar",
          "system/plugins/ch.ivyteam.ivy.webservice.execution_*.jar",
          "system/plugins/ch.ivyteam.ivy.webservice.process_*.jar")),
  WEBAPP_CONTAINER("WEBAPP_CONTAINER", ImmutableList.of("webapps/ivy/WEB-INF/lib/**/*.jar"), null),
  WS_CALL_AXIS2_CONTAINER(
      "WS_CALL_AXIS2_CONTAINER",
      ImmutableList.of(
          "system/plugins/ch.ivyteam.ivy.webservice.configuration_*.jar",
          "system/plugins/ch.ivyteam.ivy.webservice.exec.cxf_*.jar",
          "system/plugins/ch.ivyteam.ivy.webservice.execution_*.jar"),
      null),
  WS_PROCESS_CONTAINER(
      "WS_PROCESS_CONTAINER",
      ImmutableList.of("system/plugins/ch.ivyteam.ivy.webservice.process_*.jar"),
      null),
  RULE_ENGINE_CONTAINER(
      "RULE_ENGINE_CONTAINER",
      ImmutableList.of(
          "system/plugins/ch.ivyteam.lib.drools_*/lib/*.jar",
          "system/plugins/ch.ivyteam.lib.drools_*/patch.jar"),
      null),
  INTERNAL_WEB_CONTAINER("org.eclipse.jst.j2ee.internal.web.container", null, null);

  private String name;
  private List<String> paths;
  private List<String> excludedPaths;
}
