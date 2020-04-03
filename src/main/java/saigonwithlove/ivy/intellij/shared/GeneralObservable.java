package saigonwithlove.ivy.intellij.shared;

import java.util.Observable;

public class GeneralObservable extends Observable {
  public void notifyObservers(Object object) {
    super.setChanged();
    super.notifyObservers(object);
  }
}
