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
      new ImmutableList.Builder<String>().add("/lib/ivy/*.jar").add("/lib/shared/*.jar").build(),
      new ImmutableList.Builder<String>()
          .add("/lib/shared/ulc-base-server.jar")
          .add("/lib/shared/ulc-base-client.jar")
          .add("/lib/shared/ulc-base-trusted.jar")
          .add("/lib/shared/ulc-servlet-client.jar")
          .add("/lib/shared/ulc-local-server.jar")
          .add("/lib/shared/ulc-deployment-key.jar")
          .build()),
  WEBAPP_CONTAINER("WEBAPP_CONTAINER", ImmutableList.of("/webapps/ivy/WEB-INF/lib/*.jar"), null),
  ULC_CONTAINER(
      "ULC_CONTAINER",
      new ImmutableList.Builder<String>()
          .add("/lib/shared/ulc-base-server.jar")
          .add("/lib/shared/ulc-base-client.jar")
          .add("/lib/shared/ulc-base-trusted.jar")
          .add("/lib/shared/ulc-servlet-client.jar")
          .add("/lib/shared/ulc-local-server.jar")
          .add("/lib/shared/ulc-deployment-key.jar")
          .build(),
      null),
  INTERNAL_WEB_CONTAINER("org.eclipse.jst.j2ee.internal.web.container", null, null);

  private String name;
  private List<String> paths;
  private List<String> excludedPaths;
}
