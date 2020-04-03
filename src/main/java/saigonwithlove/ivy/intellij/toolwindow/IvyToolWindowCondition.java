package saigonwithlove.ivy.intellij.toolwindow;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Condition;

public class IvyToolWindowCondition implements Condition<Project> {
  @Override
  public boolean value(Project project) {
    return true;
  }
}
