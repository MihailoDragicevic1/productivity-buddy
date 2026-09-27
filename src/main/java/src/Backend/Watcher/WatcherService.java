package src.Backend.Watcher;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import src.Backend.Scanner.ProcessScanner;
import src.Backend.Scanner.Scanner;
import src.Backend.main.ProcessData;
import src.Backend.main.ProcessKey;
import src.Backend.utils.ProcessInfoJsonUtil;
import src.Backend.utils.ProcessInfoJsonUtil.JsonProcessInfo;

public class WatcherService extends Thread {

    private final ExecutorService fileService;
    private final ConcurrentHashMap<ProcessKey, ProcessData> map;
    private final String mappingFile;
    private final Path filePath;
    private final Scanner scanner;

    public WatcherService(ExecutorService es,
                          ConcurrentHashMap<ProcessKey, ProcessData> map,
                          String mappingFile, Scanner scanner) {
        this.fileService = es;
        this.map = map;
        this.mappingFile = mappingFile;
        this.filePath = Paths.get(mappingFile);
        this.scanner = scanner;
    }

    @Override
    public void run() {
        try {
            Path absoluteFilePath = filePath.toAbsolutePath();
            Path dir = absoluteFilePath.getParent();

            if (dir == null) {
                dir = Paths.get(".").toAbsolutePath().normalize();
            }

            if (!Files.exists(absoluteFilePath)) {
                ProcessInfoJsonUtil.createEmptyJsonFile(absoluteFilePath);
            }

            WatchService watcher = FileSystems.getDefault().newWatchService();
            dir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);

            while (!Thread.currentThread().isInterrupted()) {
                WatchKey key = watcher.take();

                for (WatchEvent<?> event : key.pollEvents()) {
                    String changed = event.context().toString();

                    if (changed.equals(Paths.get(mappingFile).getFileName().toString())) {
                        Future<String> content = fileService.submit(() -> Files.readString(absoluteFilePath));

                        try {
                            String json = content.get();
                            List<JsonProcessInfo> infos = ProcessInfoJsonUtil.parseJson(json);
                            applyJsonDataToMap(infos);
                        } catch (ExecutionException e) {
                            e.printStackTrace();
                        }
                    }
                    //if(changed.equals(scanner.))

                }


                boolean valid = key.reset();
                if (!valid) {
                    break;
                }
            }

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void applyJsonDataToMap(List<JsonProcessInfo> infos) {
        for (JsonProcessInfo info : infos) {
            for (ProcessKey key : map.keySet()) {
                ProcessData data = map.get(key);

                if (data == null) {
                    continue;
                }

                if (data.getName() == null) {
                    continue;
                }

                if (data.getName().equalsIgnoreCase(info.originalName)) {
                    data.setAlias(info.aliasName);
                    data.setCategory(info.category);
                    data.setTrackingFrozen(info.isTrackingFreezed);
                    data.setTotalTimeSeconds(info.totalTimeSeconds);

                    if (!data.isTrackingFrozen()) {
                        data.setLastUpdated(Instant.now());
                    }
                }
            }
        }
    }
}