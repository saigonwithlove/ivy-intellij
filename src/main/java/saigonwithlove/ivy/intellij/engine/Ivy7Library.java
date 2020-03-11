package saigonwithlove.ivy.intellij.engine;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Ivy7Library implements IvyLibrary {
  IVY_CONTAINER(
      "IVY_CONTAINER",
      ImmutableList.of(
          "/system/lib/boot/**/*.jar",
          "/system/plugins/**/*.jar",
          "/system/configuration/org.eclipse.osgi/**/*.jar"),
      ImmutableList.of(
          "/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc/**/*.jar",
          "/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc_deploy/**/*.jar",
          "/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc_ext/**/*.jar",
          "/system/configuration/org.eclipse.osgi/70/0/.cp/lib/axis2-1.3_patched/**/*.jar",
          "/system/configuration/org.eclipse.osgi/72/0/.cp/lib/mvn/**/*.jar",
          "/system/plugins/ch.ivyteam.ivy.rule.engine.libs_*.jar",
          "/system/plugins/ch.ivyteam.ivy.rule.engine_*.jar")),
  WEBAPP_CONTAINER("WEBAPP_CONTAINER", ImmutableList.of("/webapps/ivy/WEB-INF/lib/**/*.jar"), null),
  ULC_CONTAINER(
      "ULC_CONTAINER",
      ImmutableList.of(
          "/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc/**/*.jar",
          "/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc_deploy/**/*.jar",
          "/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc_ext/**/*.jar"),
      null),
  WS_CALL_AXIS2_CONTAINER(
      "WS_CALL_AXIS2_CONTAINER",
      ImmutableList.of(
          "/system/configuration/org.eclipse.osgi/70/0/.cp/lib/axis2-1.3_patched/**/*.jar"),
      null),
  WS_PROCESS_CONTAINER(
      "WS_PROCESS_CONTAINER",
      ImmutableList.of("/system/configuration/org.eclipse.osgi/72/0/.cp/lib/mvn/**/*.jar"),
      null),
  RULE_ENGINE_CONTAINER(
      "RULE_ENGINE_CONTAINER",
      ImmutableList.of(
          "/system/plugins/ch.ivyteam.ivy.rule.engine.libs_*.jar",
          "/system/plugins/ch.ivyteam.ivy.rule.engine_*.jar"),
      null),
  INTERNAL_WEB_CONTAINER("org.eclipse.jst.j2ee.internal.web.container", null, null);

  private String name;
  private List<String> paths;
  private List<String> excludedPaths;
}
