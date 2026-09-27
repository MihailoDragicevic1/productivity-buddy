package src.Backend.Analisys;

import src.Backend.main.ProcessData;
import src.Backend.main.ProcessKey;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class AnalisysModule extends Thread {

    private final ConcurrentHashMap<ProcessKey, ProcessData> map;

    private long workTimeSeconds;
    private long funTimeSeconds;
    private long otherTimeSeconds;
    private long uncategorizedTimeSeconds;

    private List<ProcessData> topProcessesByTime = new ArrayList<>();

    public AnalisysModule(ConcurrentHashMap<ProcessKey, ProcessData> map) {
        this.map = map;
    }

    public long getWorkTimeSeconds() {
        return workTimeSeconds;
    }

    public long getFunTimeSeconds() {
        return funTimeSeconds;
    }

    public long getOtherTimeSeconds() {
        return otherTimeSeconds;
    }

    public long getUncategorizedTimeSeconds() {
        return uncategorizedTimeSeconds;
    }


    private void refreshStats() {
        ConcurrentHashMap<String, ProcessData> uniqueByName = new ConcurrentHashMap<>();

        for (ProcessData data : map.values()) {
            if (data == null) {
                continue;
            }

            String nameKey = data.getName();
            if (nameKey == null || nameKey.isBlank()) {
                continue;
            }

            ProcessData existing = uniqueByName.get(nameKey);

            if (existing == null || data.getTotalTimeSeconds() > existing.getTotalTimeSeconds()) {
                uniqueByName.put(nameKey, data);
            }
        }

        long work = 0;
        long fun = 0;
        long other = 0;
        long uncategorized = 0;

        List<ProcessData> allProcesses = new ArrayList<>(uniqueByName.values());

        for (ProcessData data : allProcesses) {
            String category = data.getCategory();
            long time = data.getTotalTimeSeconds();

            if (category == null || category.isBlank()) {
                uncategorized += time;
            }
            else if (category.equalsIgnoreCase("Work")) {
                work += time;
            }
            else if (category.equalsIgnoreCase("Fun")) {
                fun += time;
            }
            else if (category.equalsIgnoreCase("Other")) {
                other += time;
            }
            else {
                uncategorized += time;
            }
        }

        allProcesses.sort(Comparator.comparingLong(ProcessData::getTotalTimeSeconds).reversed());

        if (allProcesses.size() > 10) {
            topProcessesByTime = new ArrayList<>(allProcesses.subList(0, 10));
        } else {
            topProcessesByTime = allProcesses;
        }

        workTimeSeconds = work;
        funTimeSeconds = fun;
        otherTimeSeconds = other;
        uncategorizedTimeSeconds = uncategorized;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                refreshStats();
                Thread.sleep(2000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}