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
      ImmutableList.of(
          "/system/plugins/",
          "/system/plugins/ch.ivyteam.commons.lib_8.0.0.202001081608/lib",
          "/system/plugins/ch.ivyteam.lib.classgraph_4.8.52.202001081608/lib",
          "/system/plugins/ch.ivyteam.lib.cxf.webservice_3.3.4.202001081608/lib",
          "/system/plugins/ch.ivyteam.lib.hibernate_7.3.0.202001081608/lib",
          "/system/plugins/ch.ivyteam.lib.javax.activation_1.2.1.202001081608/lib",
          "/system/plugins/ch.ivyteam.lib.jaxws_2.3.1.202001081608/lib",
          "/system/plugins/ch.ivyteam.lib.jersey_2.29.1.202001081608/lib",
          "/system/plugins/ch.ivyteam.lib.poi_4.1.1.202001081608/lib",
          "/system/plugins/ch.ivyteam.lib.poi.ooxml_4.1.1.202001081608/lib",
          "/system/plugins/ch.ivyteam.lib.util_8.0.0.202001081608/lib",
          "/system/plugins/ch.ivyteam.tomcat_9.0.27.202001081608/lib"),
      ImmutableList.of(
          "/system/plugins/ch.ivyteam.ivy.webservice.configuration_8.0.2.202001240914.jar",
          "/system/plugins/ch.ivyteam.ivy.webservice.exec.cxf_8.0.2.202001240914.jar",
          "/system/plugins/ch.ivyteam.ivy.webservice.execution_8.0.2.202001240914.jar",
          "/system/plugins/ch.ivyteam.ivy.webservice.process_8.0.2.202001240914.jar")),
  WEBAPP_CONTAINER("WEBAPP_CONTAINER", ImmutableList.of("/webapps/ivy/WEB-INF/lib/"), null),
  WS_CALL_AXIS2_CONTAINER(
      "WS_CALL_AXIS2_CONTAINER",
      ImmutableList.of(
          "/system/plugins/ch.ivyteam.ivy.webservice.configuration_8.0.2.202001240914.jar",
          "/system/plugins/ch.ivyteam.ivy.webservice.exec.cxf_8.0.2.202001240914.jar",
          "/system/plugins/ch.ivyteam.ivy.webservice.execution_8.0.2.202001240914.jar"),
      null),
  WS_PROCESS_CONTAINER(
      "WS_PROCESS_CONTAINER",
      ImmutableList.of("/system/plugins/ch.ivyteam.ivy.webservice.process_8.0.2.202001240914.jar"),
      null),
  RULE_ENGINE_CONTAINER(
      "RULE_ENGINE_CONTAINER",
      ImmutableList.of(
          "/system/plugins/ch.ivyteam.lib.drools_7.29.0.202001081608/lib",
          "/system/plugins/ch.ivyteam.lib.drools_7.29.0.202001081608/patch.jar"),
      null),
  INTERNAL_WEB_CONTAINER("org.eclipse.jst.j2ee.internal.web.container", null, null);

  private String name;
  private List<String> paths;
  private List<String> excludedPaths;
}
