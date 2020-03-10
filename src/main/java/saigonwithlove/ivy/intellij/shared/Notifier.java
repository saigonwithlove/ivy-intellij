package saigonwithlove.ivy.intellij.shared;

import com.intellij.notification.NotificationDisplayType;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.project.Project;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Notifier {
  private final NotificationGroup GROUP =
      new NotificationGroup("Ivy", NotificationDisplayType.BALLOON, true);

  public void info(@NotNull Project project, @NotNull String content) {
    GROUP.createNotification(content, NotificationType.INFORMATION).notify(project);
  }

  public void info(@NotNull Project project, @NotNull AnAction action, @NotNull String content) {
    GROUP
        .createNotification(content, NotificationType.INFORMATION)
        .addAction(action)
        .notify(project);
  }
}
