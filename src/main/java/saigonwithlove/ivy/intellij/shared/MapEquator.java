package saigonwithlove.ivy.intellij.shared;

import java.util.Map;
import java.util.function.BiPredicate;

public class MapEquator<K, V> implements BiPredicate<Map<K, V>, Map<K, V>> {
  @Override
  public boolean test(Map<K, V> a, Map<K, V> b) {
    if (a == null || b == null) {
      // one or both a, b are null
      return a == b;
    } else if (a.size() > 0 || b.size() > 0) {
      // one or both a, b are not empty
      return new CollectionEquator<Map.Entry<K, V>>().test(a.entrySet(), b.entrySet());
    } else {
      // both a, b are empty
      return true;
    }
  }
}
