package saigonwithlove.ivy.intellij.shared;

import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Notifier {
  private static final NotificationGroup GROUP =
      NotificationGroupManager.getInstance().getNotificationGroup("Ivy");

  public static void info(@NotNull Project project, @NotNull String content) {
    GROUP.createNotification(content, NotificationType.INFORMATION).notify(project);
  }

  public static void info(
      @NotNull Project project, @NotNull AnAction action, @NotNull String content) {
    GROUP
        .createNotification(content, NotificationType.INFORMATION)
        .addAction(action)
        .notify(project);
  }
}
