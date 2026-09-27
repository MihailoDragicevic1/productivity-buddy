package src.Backend.Scanner;

import src.Backend.main.ProcessData;
import src.Backend.main.ProcessKey;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class Scanner extends Thread {

	private final ConcurrentHashMap<ProcessKey, ProcessData> map;
	private final long sleepTime;
	private final ForkJoinPool forkJoinPool;

	private volatile boolean running = true;
	private final  String fajlString;
	public Scanner(java.util.concurrent.ExecutorService e, ConcurrentHashMap<ProcessKey, ProcessData> map, long sleepTime,String fajlString) {
		this.map = map;
		this.sleepTime = sleepTime;
		this.forkJoinPool = new ForkJoinPool();
		this.fajlString = fajlString;
	}

	public void stopScanner() {
		running = false;
		this.interrupt();
	}

	@Override
	public void run() {
		try {
			while (running) {
				List<ProcessHandle> processes = ProcessHandle.allProcesses()
						.collect(Collectors.toList());

				Set<String> blacklist = readBlacklist();
				System.out.println(blacklist);

				List<ProcessData> freshData = forkJoinPool.invoke(
						new ProcessScanner(processes, 0, processes.size(),blacklist)
				);

				Set<ProcessKey> activeKeys = new HashSet<>();

				for (ProcessData newData : freshData) {
					ProcessKey key = new ProcessKey(newData.getPid(), newData.getStartTime());
					activeKeys.add(key);

					ProcessData oldData = map.get(key);

					if (oldData == null) {
						map.put(key, newData);
					} else {
						ProcessData mergedData = mergeProcessData(oldData, newData);
						map.put(key, mergedData);
					}
				}

				removeDeadProcesses(activeKeys);

				Thread.sleep(sleepTime);
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} finally {
			forkJoinPool.shutdown();
		}
	}

	private ProcessData mergeProcessData(ProcessData oldData, ProcessData newData) {
		if (oldData.isTrackingFrozen()) {
			oldData.setAlive(newData.isAlive());
			oldData.setPh(newData.getPh());
			oldData.setPid(newData.getPid());
			oldData.setName(newData.getName());
			oldData.setStartTime(newData.getStartTime());
			return oldData;
		}

		long additionalSeconds = calculateAdditionalSeconds(oldData.getLastUpdated(), newData.getLastUpdated());
		double cpuUsage = calculateCpuUsage(oldData, newData);

		newData.setAlias(oldData.getAlias());
		newData.setCategory(oldData.getCategory());
		newData.setTrackingFrozen(oldData.isTrackingFrozen());
		newData.setTotalTimeSeconds(oldData.getTotalTimeSeconds() + additionalSeconds);
		newData.setCpuUsage(cpuUsage);

		return newData;
	}

	private long calculateAdditionalSeconds(Instant oldLastUpdated, Instant newLastUpdated) {
		if (oldLastUpdated == null || newLastUpdated == null) {
			return 0;
		}

		long seconds = Duration.between(oldLastUpdated, newLastUpdated).getSeconds();
		return Math.max(seconds, 0);
	}

	private double calculateCpuUsage(ProcessData oldData, ProcessData newData) {

		long oldUpTime = oldData.getUpTimeMillis();
		long newUpTime = newData.getUpTimeMillis();

		long oldCpuTime = oldData.getKernelTime() + oldData.getUserTime();
		long newCpuTime = newData.getKernelTime() + newData.getUserTime();

		long deltaUpTime = newUpTime - oldUpTime;
		long deltaCpuTime = newCpuTime - oldCpuTime;

		if (deltaUpTime <= 0 || deltaCpuTime < 0) {
			return 0.0;
		}

		double cpu = ((double) deltaCpuTime / (double) deltaUpTime) * 100.0;

		if (cpu < 0.0) {
			return 0.0;
		}

		return Math.round(cpu * 100.0) / 100.0;
	}

	private void removeDeadProcesses(Set<ProcessKey> activeKeys) {
		for (ProcessKey existingKey : map.keySet()) {
			if (!activeKeys.contains(existingKey)) {
				map.remove(existingKey);
			}
		}
	}
	private Set<String> readBlacklist() {
		try {
			Path path = Path.of(fajlString);

			if (!Files.exists(path)) {
				Files.writeString(path, "");
			}

			List<String> lines = Files.readAllLines(path);

			Set<String> blacklist = new HashSet<>();

			for (String line : lines) {

				if (line == null) {
					continue;
				}

				String trimmed = line.trim();

				if (trimmed.isBlank()) {
					continue;
				}

				blacklist.add(trimmed.toLowerCase());
			}

			return blacklist;

		} catch (Exception e) {
			return new HashSet<>();
		}
	}
}