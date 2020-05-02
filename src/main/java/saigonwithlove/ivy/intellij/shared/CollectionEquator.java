package saigonwithlove.ivy.intellij.shared;

import java.util.Collection;
import java.util.function.BiPredicate;

public class CollectionEquator<T> implements BiPredicate<Collection<T>, Collection<T>> {
  @Override
  public boolean test(Collection<T> a, Collection<T> b) {
    if (a == null || b == null) {
      // one or both a, b are null
      return a == b;
    } else if (a.size() > 0 || b.size() > 0) {
      // one or both a, b are not empty
      return a.size() == b.size() && a.containsAll(b);
    } else {
      // both a, b are empty
      return true;
    }
  }
}
