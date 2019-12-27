package saigonwithlove.ivy.intellij.shared;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.diagnostic.Logger;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringUtils;
import saigonwithlove.ivy.intellij.engine.IvyEngineService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@UtilityClass
public class IvyEngine {
    public static final String ENGINE_LOG_PATH = "/logs/Axon.ivy Engine.console";
    public static final Pattern ENGINE_PATTERN = Pattern.compile("AxonIvyEngine");
    private static final Logger LOG = Logger.getInstance("#" + IvyEngine.class.getCanonicalName());

    public static boolean isEngine(String directoryUri) {
        File binPath = new File(directoryUri + "/bin");
        if(binPath.isDirectory()) {
            for(File file : binPath.listFiles()) {
                if(ENGINE_PATTERN.matcher(file.getName()).find()) {
                    return true;
                }
            }
            return false;
        }
        return false;
    }

    public static boolean isOsgiFolderExist(String directoryUri) {
        File osgiPath = new File(directoryUri + "/system/configuration/org.eclipse.osgi");
        return osgiPath.isDirectory() && osgiPath.exists();
    }

    public static void cleanUpEngineLog(String engineDirectory) {
        try {
            Path engineLog = Paths.get(engineDirectory + ENGINE_LOG_PATH);
            Files.deleteIfExists(engineLog);
        } catch (IOException e) {
            LOG.error("Engine log not found", e);
        }
    }

    public boolean engineUpAndRun(String engineDirectory) {
        Path engineLog = Paths.get(engineDirectory + IvyEngine.ENGINE_LOG_PATH);
        boolean engineRun = false;
        while (!engineRun) {
            try {
                if (Files.exists(engineLog)) {
                    List<String> allLines = Files.readAllLines(engineLog);
                    for (String line : allLines) {
                        if (line.contains("Axon.ivy Engine is running and ready to serve.")) {
                            engineRun = true;
                        }
                    }
                    Thread.sleep(ThreadLocalRandom.current().nextInt(100, 1000));
                }
            } catch (IOException | InterruptedException e) {
                LOG.error("Engine check error", e);
                return false;
            }
        }
        return true;
    }

}
