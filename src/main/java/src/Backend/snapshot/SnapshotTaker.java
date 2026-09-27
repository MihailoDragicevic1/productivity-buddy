package src.Backend.snapshot;

import src.Backend.main.ProcessData;
import src.Backend.main.ProcessKey;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

public class SnapshotTaker extends Thread {

    private long snapshotInterval;
    private ExecutorService ioTask;
    private final ConcurrentHashMap<ProcessKey, ProcessData> mainMap;
    private ArrayList<Instant> reservedTime;
    private long secondsPassed = 0;
    private LocalTime lastTriggeredReservedTime = null;

    public SnapshotTaker(long snapshotInterval,
                         ExecutorService ioTask,
                         ConcurrentHashMap<ProcessKey, ProcessData> mainMap,
                         ArrayList<Instant> reservedTime) {
        this.snapshotInterval = snapshotInterval;
        this.ioTask = ioTask;
        this.mainMap = mainMap;
        this.reservedTime = reservedTime;
    }

    public void writeMapToCsv(ConcurrentHashMap<ProcessKey, ProcessData> tempMap, String fileName) {
        try (BufferedWriter writer = Files.newBufferedWriter(Path.of(fileName))) {

            writer.write("timestamp,pid,process_name,cpu_usage,ram_usage,category,alias_name,total_time_seconds,is_tracking_frozen");
            writer.newLine();

            String timestamp = Instant.now().toString();

            for (ProcessKey key : tempMap.keySet()) {
                ProcessData data = tempMap.get(key);

                if (data == null) {
                    continue;
                }

                String line = String.join(",",
                        safe(timestamp),
                        String.valueOf(key.getPid()),
                        safe(data.getName()),
                        String.valueOf(data.getCpuUsage()),
                        String.valueOf(data.getRamUsage()),
                        safe(data.getCategory()),
                        safe(data.getAlias()),
                        String.valueOf(data.getTotalTimeSeconds()),
                        String.valueOf(data.isTrackingFrozen())
                );

                writer.write(line);
                writer.newLine();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String safe(String value) {
        if (value == null) {
            return "";
        }

        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }

        return escaped;
    }

    private boolean isReservedTime() {
        Instant now = Instant.now();
        LocalTime currentTime = now.atZone(ZoneId.systemDefault()).toLocalTime().withNano(0);

        for (Instant reservedInstant : reservedTime) {
            LocalTime reservedLocalTime = reservedInstant.atZone(ZoneId.systemDefault()).toLocalTime().withNano(0);

            if (reservedLocalTime.equals(currentTime)) {
                if (reservedLocalTime.equals(lastTriggeredReservedTime)) {
                    return false;
                }

                lastTriggeredReservedTime = reservedLocalTime;
                return true;
            }
        }

        if (lastTriggeredReservedTime != null && !lastTriggeredReservedTime.equals(currentTime)) {
            lastTriggeredReservedTime = null;
        }

        return false;
    }

    private String generateSnapshotFileName() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss");
        String timePart = LocalDateTime.now().format(formatter);
        return "snapshot_" + timePart + ".csv";
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {

                boolean shouldTakeSnapshot = false;

                if (snapshotInterval > 0 && secondsPassed >= snapshotInterval) {
                    shouldTakeSnapshot = true;
                    secondsPassed = 0;
                }

                if (isReservedTime()) {
                    shouldTakeSnapshot = true;
                }

                if (shouldTakeSnapshot) {
                    ConcurrentHashMap<ProcessKey, ProcessData> tempMap;

                    synchronized (mainMap) {
                        tempMap = new ConcurrentHashMap<>(mainMap);
                    }

                    String fileName = generateSnapshotFileName();

                    ioTask.submit(() -> writeMapToCsv(tempMap, fileName));
                }

                Thread.sleep(1000);
                secondsPassed++;

            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}