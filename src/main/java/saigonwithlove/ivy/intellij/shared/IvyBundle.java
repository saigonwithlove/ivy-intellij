package saigonwithlove.ivy.intellij.shared;

import com.intellij.CommonBundle;
import java.lang.ref.Reference;
import java.lang.ref.SoftReference;
import java.util.ResourceBundle;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.PropertyKey;

@UtilityClass
public class IvyBundle {
  @NonNls private static final String BUNDLE = "messages.IvyBundle";
  private static Reference<ResourceBundle> ivyBundle;

  public static String message(
      @NotNull @PropertyKey(resourceBundle = BUNDLE) String key, @NotNull Object... params) {
    return CommonBundle.message(getBundle(), key, params);
  }

  private static ResourceBundle getBundle() {
    ResourceBundle bundle = com.intellij.reference.SoftReference.dereference(ivyBundle);
    if (bundle == null) {
      bundle = ResourceBundle.getBundle(BUNDLE);
      ivyBundle = new SoftReference<>(bundle);
    }
    return bundle;
  }
}
