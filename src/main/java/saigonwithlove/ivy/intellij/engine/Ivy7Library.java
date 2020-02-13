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
          "/system/lib/boot/", "/system/plugins/", "/system/configuration/org.eclipse.osgi/"),
      new ImmutableList.Builder<String>()
          .add("/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc/")
          .add("/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc_deploy/")
          .add("/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc_ext/")
          .add("/system/configuration/org.eclipse.osgi/70/0/.cp/lib/axis2-1.3_patched/")
          .add("/system/configuration/org.eclipse.osgi/72/0/.cp/lib/mvn/")
          .add("/system/plugins/ch.ivyteam.ivy.rule.engine.libs_7.0.0.201809271142.jar")
          .add("/system/plugins/ch.ivyteam.ivy.rule.engine_7.0.0.201809271142.jar")
          .build()),
  WEBAPP_CONTAINER("WEBAPP_CONTAINER", ImmutableList.of("/webapps/ivy/WEB-INF/lib/"), null),
  ULC_CONTAINER(
      "ULC_CONTAINER",
      ImmutableList.of(
          "/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc/",
          "/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc_deploy/",
          "/system/configuration/org.eclipse.osgi/76/0/.cp/lib_ulc_ext/"),
      null),
  WS_CALL_AXIS2_CONTAINER(
      "WS_CALL_AXIS2_CONTAINER",
      ImmutableList.of("/system/configuration/org.eclipse.osgi/70/0/.cp/lib/axis2-1.3_patched/"),
      null),
  WS_PROCESS_CONTAINER(
      "WS_PROCESS_CONTAINER",
      ImmutableList.of("/system/configuration/org.eclipse.osgi/72/0/.cp/lib/mvn/"),
      null),
  RULE_ENGINE_CONTAINER(
      "RULE_ENGINE_CONTAINER",
      ImmutableList.of(
          "/system/plugins/ch.ivyteam.ivy.rule.engine.libs_7.0.0.201809271142.jar",
          "/system/plugins/ch.ivyteam.ivy.rule.engine_7.0.0.201809271142.jar"),
      null),
  INTERNAL_WEB_CONTAINER("org.eclipse.jst.j2ee.internal.web.container", null, null);

  private String name;
  private List<String> paths;
  private List<String> excludedPaths;
}
