package saigonwithlove.ivy.intellij.shared;

import com.google.common.base.Preconditions;
import com.intellij.openapi.vfs.VirtualFile;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

@UtilityClass
public class Configurations {
  private static final Pattern DEFAULT_VALUE_FILE_PATTERN =
      Pattern.compile("^val.*guid=\\{([0-9A-Z]+)\\}.*isdefault=\\{true\\}", Pattern.MULTILINE);
  private static final Pattern VALUE_PATTERN = Pattern.compile("/value \"(.*)\"/description", Pattern.MULTILINE);

  @NotNull
  public static Configuration parseGlobalVariable(@NotNull VirtualFile globalVariableDirectory) {
    String name = globalVariableDirectory.getName();
    String value =
        Optional.of(globalVariableDirectory)
            .map(Configurations::getValueFile)
            .map(Configurations::getValue)
            .orElse(StringUtils.EMPTY);
    return new Configuration(name, value);
  }

  @NotNull
  private VirtualFile getValueFile(@NotNull VirtualFile globalVariableDirectory) {
    VirtualFile contentObjectDefinition = globalVariableDirectory.findChild("co.meta");
    if (contentObjectDefinition != null) {
      try {
        Matcher matcher =
            DEFAULT_VALUE_FILE_PATTERN.matcher(
                new String(contentObjectDefinition.contentsToByteArray()));
        if (matcher.find()) {
          String valueFileName = matcher.group(1);
          return Preconditions.checkNotNull(
              globalVariableDirectory.findChild(valueFileName + ".data"));
        }
      } catch (IOException ex) {
        throw new RuntimeException(
            "Could not read content definition of global variable: " + globalVariableDirectory);
      }
    }
    throw new NoSuchElementException(
        "Could not find value file of global variable: " + globalVariableDirectory);
  }

  @NotNull
  private String getValue(@NotNull VirtualFile valueFile) {
    try {
      Matcher matcher = VALUE_PATTERN.matcher(new String(valueFile.contentsToByteArray()));
      if (matcher.find()) {
        return matcher.group(1);
      }
    } catch (IOException ex) {
      throw new RuntimeException("Could not read value of global variable: " + valueFile);
    }
    return StringUtils.EMPTY;
  }
}
