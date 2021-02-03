package saigonwithlove.ivy.intellij.shared;

import com.intellij.AbstractBundle;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

public class IvyBundle extends AbstractBundle {
  @NonNls private static final String BUNDLE = "messages.IvyBundle";
  private static Reference<IvyBundle> ivyBundle;

  public IvyBundle() {
    super(BUNDLE);
  }

  public static String message(
      @NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
    return getBundle().getMessage(key, params);
  }

  private static IvyBundle getBundle() {
    IvyBundle bundle = com.intellij.reference.SoftReference.dereference(ivyBundle);
    if (bundle == null) {
      bundle = new IvyBundle();
      ivyBundle = new SoftReference<>(bundle);
    }
    return bundle;
  }
}
