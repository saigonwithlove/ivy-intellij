package saigonwithlove.ivy.intellij.action;

import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NotNull;

public class OpenUrlAction extends AnAction {
  private String url;

  public OpenUrlAction(@NotNull String text, @NotNull String url) {
    super(text, url, AllIcons.Actions.More);
    this.url = url;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent event) {
    BrowserUtil.browse(this.url);
  }
}
