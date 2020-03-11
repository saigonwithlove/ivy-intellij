package saigonwithlove.ivy.intellij.engine;

import com.google.common.collect.ImmutableList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Ivy6Library implements IvyLibrary {
  IVY_CONTAINER(
      "IVY_CONTAINER",
      ImmutableList.of("lib/ivy/**/*.jar", "lib/shared/**/*.jar"),
      ImmutableList.of(
          "lib/shared/ulc-base-server.jar",
          "lib/shared/ulc-base-client.jar",
          "lib/shared/ulc-base-trusted.jar",
          "lib/shared/ulc-servlet-client.jar",
          "lib/shared/ulc-local-server.jar",
          "lib/shared/ulc-deployment-key.jar")),
  WEBAPP_CONTAINER("WEBAPP_CONTAINER", ImmutableList.of("webapps/ivy/WEB-INF/lib/**/*.jar"), null),
  ULC_CONTAINER(
      "ULC_CONTAINER",
      ImmutableList.of(
          "lib/shared/ulc-base-server.jar",
          "lib/shared/ulc-base-client.jar",
          "lib/shared/ulc-base-trusted.jar",
          "lib/shared/ulc-servlet-client.jar",
          "lib/shared/ulc-local-server.jar",
          "lib/shared/ulc-deployment-key.jar"),
      null),
  INTERNAL_WEB_CONTAINER("org.eclipse.jst.j2ee.internal.web.container", null, null);

  private String name;
  private List<String> paths;
  private List<String> excludedPaths;
}
