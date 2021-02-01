package saigonwithlove.ivy.intellij.settings;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import io.reactivex.rxjava3.core.Observer;
import io.reactivex.rxjava3.disposables.Disposable;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import saigonwithlove.ivy.intellij.shared.CollectionEquator;
import saigonwithlove.ivy.intellij.shared.MapEquator;

@AllArgsConstructor
@RequiredArgsConstructor
public class CacheObserver<T> implements Observer<T> {
  private static final Logger LOG =
      Logger.getInstance("#" + CacheObserver.class.getCanonicalName());

  @NonNull private final String name;
  @NonNull private final Handler<T> handler;
  private T value;

  @Override
  public void onSubscribe(@io.reactivex.rxjava3.annotations.NonNull Disposable disposable) {}

  @Override
  public void onNext(@io.reactivex.rxjava3.annotations.NonNull T newValue) {
    if (!equals(this.value, newValue)) {
      LOG.info(
          MessageFormat.format(
              "{0}: oldValue: {1}, newValue: {2}", this.name, this.value, newValue));
      this.value = newValue;
      ApplicationManager.getApplication().invokeLater(() -> this.handler.accept(this.value));
    }
  }

  @Override
  public void onError(@io.reactivex.rxjava3.annotations.NonNull Throwable ex) {}

  @Override
  public void onComplete() {}

  public interface Handler<T> extends Consumer<T> {}

  private static boolean equals(Object a, Object b) {
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
}
