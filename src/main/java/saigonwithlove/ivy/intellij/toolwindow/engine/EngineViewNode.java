package saigonwithlove.ivy.intellij.toolwindow.engine;

import javax.swing.Icon;

public interface EngineViewNode<T> {
  public Icon getIcon();

  public String getLabel();

  public T getUserObject();
}
