package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.diagnostic.Logger;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.function.Consumer;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import saigonwithlove.ivy.intellij.shared.CollectionEquator;
import saigonwithlove.ivy.intellij.shared.MapEquator;

@AllArgsConstructor
@RequiredArgsConstructor
public class CacheObserver<T> implements Observer {
  private static final Logger LOG =
      Logger.getInstance("#" + CacheObserver.class.getCanonicalName());

  @NonNull private String name;
  @NonNull private Converter<T> converter;
  @NonNull private Updater<T> updater;
  private T value;

  @Override
  public void update(Observable observable, Object model) {
    T newValue = converter.apply((PreferenceService.Cache) model);
    if (!equals(this.value, newValue)) {
      LOG.info(
          MessageFormat.format(
              "{0}: oldValue: {1}, newValue: {2}", this.name, this.value, newValue));
      this.value = newValue;
      updater.accept(this.value);
    }
  }

  private boolean equals(Object a, Object b) {
    if (a == null || b == null) {
      return a == b;
    } else if (a instanceof Collection) {
      return new CollectionEquator<>().test((Collection) a, (Collection) b);
    } else if (a instanceof Map) {
      return new MapEquator<>().test((Map) a, (Map) b);
    } else if (a instanceof Pair) {
      Pair x = (Pair) a;
      Pair y = (Pair) b;
      return equals(x.getLeft(), y.getLeft()) && equals(x.getRight(), y.getRight());
    } else {
      return a.equals(b);
    }
  }

  public static interface Converter<T> extends Function<PreferenceService.Cache, T> {}

  public static interface Updater<T> extends Consumer<T> {}
}
