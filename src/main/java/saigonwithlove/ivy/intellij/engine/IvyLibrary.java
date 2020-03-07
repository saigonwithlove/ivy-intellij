package saigonwithlove.ivy.intellij.engine;

import java.util.List;

public interface IvyLibrary {
  public String getName();

  public List<String> getPaths();

  public List<String> getExcludedPaths();
}
