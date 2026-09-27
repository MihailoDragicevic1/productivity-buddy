package src.Backend.main;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import src.Backend.Analisys.AnalisysModule;
import src.Backend.Scanner.ProcessScanner;
import src.Backend.Scanner.Scanner;
import src.Backend.Watcher.WatcherService;
import src.Backend.snapshot.SnapshotTaker;
import src.Backend.utils.ProcessInfoJsonUtil;
import src.Backend.utils.ProcessInfoJsonUtil.JsonProcessInfo;

public class Back {

	public ConcurrentHashMap<ProcessKey, ProcessData> mainMap =
			new ConcurrentHashMap<ProcessKey, ProcessData>();

	ExecutorService ioTasks = Executors.newSingleThreadExecutor();
	ExecutorService backendTask = Executors.newSingleThreadExecutor();

	Scanner sc;
	WatcherService ws;
	AnalisysModule am;
	SnapshotTaker st;
	ProcessScanner ps;

	public ConcurrentHashMap<ProcessKey, ProcessData> copyMap = new ConcurrentHashMap<>();

	private String configFile = "config.properties";
	private String mappingFile = "process_info.json";

	private String file = "novi.fajl.txt";

	public void startBack() {
		readConfig();
		if (sc == null || ws == null || am == null || st == null) {
			System.out.println("Backend nije pravilno inicijalizovan.");
			return;
		}

		sc.setDaemon(true);
		ws.setDaemon(true);
		am.setDaemon(true);
		st.setDaemon(true);

		ws.start();
		sc.start();
		am.start();
		st.start();

		try {
			Thread.sleep(2000);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		loadProcessInfoFromJson();
	}

	protected void readConfig() {
		Properties props = new Properties();

		try (InputStream input = Files.newInputStream(Paths.get(configFile))) {
			props.load(input);

			long scanningPeriod = Long.parseLong(props.getProperty("monitor.interval", "3000").trim());
			mappingFile = props.getProperty("mapping.file", "process_info.json").trim();
			file = props.getProperty("file", "novi.fajl.txt").trim();
			long snapshotInterval = Long.parseLong(props.getProperty("snapshot.interval", "60").trim());

			ArrayList<Instant> reservedSnapshots = new ArrayList<>();

			for (String key : props.stringPropertyNames()) {
				if (key.startsWith("snapshot.fixed_time_")) {
					String value = props.getProperty(key).trim();

					if (!value.isEmpty()) {
						LocalTime localTime = LocalTime.parse(value);
						Instant todayInstant = localTime.atDate(LocalDate.now())
								.atZone(ZoneId.systemDefault())
								.toInstant();

						reservedSnapshots.add(todayInstant);
					}
				}
			}

			sc = new Scanner(ioTasks, mainMap, scanningPeriod,file);
			ws = new WatcherService(ioTasks, mainMap, mappingFile,sc);
			am = new AnalisysModule(mainMap);
			st = new SnapshotTaker(snapshotInterval, ioTasks, mainMap, reservedSnapshots);

			Path mappingPath = Paths.get(mappingFile);
			if (!Files.exists(mappingPath)) {
				ProcessInfoJsonUtil.createEmptyJsonFile(mappingPath);
			}

		} catch (IOException | RuntimeException e) {
			e.printStackTrace();
		}
	}

	public void killProcess(ProcessKey pk) {
		backendTask.submit(() -> {
			ProcessData proc = mainMap.get(pk);

			if (proc == null) {
				return;
			}

			if (proc.getPh() == null) {
				return;
			}

			proc.getPh().destroy();
		});
	}

	public void changeName(ProcessKey pk, String newName) {
		backendTask.submit(() -> {
			ProcessData proc = mainMap.get(pk);

			if (proc == null) {
				return;
			}

			if (newName == null || newName.trim().isEmpty()) {
				return;
			}

			for (ProcessData data : mainMap.values()) {
				if (data == null) {
					continue;
				}

				if (sameOriginalProcessName(data, proc.getName())) {
					data.setAlias(newName.trim());
				}
			}

			persistProcessInfo(proc);
		});
	}

	public void freezeTracking(ProcessKey pk) {
		backendTask.submit(() -> {
			ProcessData proc = mainMap.get(pk);

			if (proc == null) {
				return;
			}

			boolean newFrozenState = !proc.isTrackingFrozen();

			for (ProcessData data : mainMap.values()) {
				if (data == null) {
					continue;
				}

				if (sameOriginalProcessName(data, proc.getName())) {
					data.setTrackingFrozen(newFrozenState);

					if (!data.isTrackingFrozen()) {
						data.setLastUpdated(Instant.now());
					}
				}
			}

			persistProcessInfo(proc);
		});
	}

	public void changeCategory(ProcessKey pk, String newCategory) {
		backendTask.submit(() -> {
			ProcessData proc = mainMap.get(pk);

			if (proc == null) {
				return;
			}

			if (newCategory == null || newCategory.trim().isEmpty()) {
				return;
			}

			for (ProcessData data : mainMap.values()) {
				if (data == null) {
					continue;
				}

				if (sameOriginalProcessName(data, proc.getName())) {
					data.setCategory(newCategory.trim());
				}
			}

			persistProcessInfo(proc);
		});
	}

	public void save(String csvName) {
		synchronized (mainMap) {
			copyMap = new ConcurrentHashMap<>(mainMap);
		}

		backendTask.submit(() -> st.writeMapToCsv(copyMap, csvName));
	}

	public void load() {
		backendTask.submit(this::loadProcessInfoFromJson);
	}

	private void loadProcessInfoFromJson() {
		try {
			Path path = Paths.get(mappingFile);

			if (!Files.exists(path)) {
				ProcessInfoJsonUtil.createEmptyJsonFile(path);
				return;
			}

			String json = Files.readString(path);
			List<JsonProcessInfo> infos = ProcessInfoJsonUtil.parseJson(json);
			applyJsonDataToMap(infos);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void applyJsonDataToMap(List<JsonProcessInfo> infos) {
		for (JsonProcessInfo info : infos) {
			boolean totalAssigned = false;

			for (ProcessData data : getUniqueProcessesByName()) {
				if (data == null || data.getName() == null) {
					continue;
				}

				if (data.getName().equalsIgnoreCase(info.originalName)) {
					for (ProcessData everyInstance : mainMap.values()) {
						if (everyInstance == null) {
							continue;
						}

						if (sameOriginalProcessName(everyInstance, info.originalName)) {
							everyInstance.setAlias(info.aliasName);
							everyInstance.setCategory(info.category);
							everyInstance.setTrackingFrozen(info.isTrackingFreezed);

							if (!everyInstance.isTrackingFrozen()) {
								everyInstance.setLastUpdated(Instant.now());
							}
						}
					}

					if (!totalAssigned) {
						data.setTotalTimeSeconds(info.totalTimeSeconds);
						totalAssigned = true;
					}
				}
			}
		}
	}

	private void persistProcessInfo(ProcessData data) {
		ioTasks.submit(() -> {
			try {
				Path path = Paths.get(mappingFile);

				if (!Files.exists(path)) {
					ProcessInfoJsonUtil.createEmptyJsonFile(path);
				}

				String json = Files.readString(path);
				List<JsonProcessInfo> infos = ProcessInfoJsonUtil.parseJson(json);

				ProcessInfoJsonUtil.upsertProcessInfo(infos, data);
				ProcessInfoJsonUtil.writeJson(path, infos);

			} catch (IOException e) {
				e.printStackTrace();
			}
		});
	}

	public void shutdown() {
		backendTask.submit(() -> {
			try {
				if (sc != null) {
					sc.stopScanner();
				}

				if (ws != null) {
					ws.interrupt();
				}

				if (am != null) {
					am.interrupt();
				}

				if (st != null) {
					st.interrupt();
				}

				synchronized (mainMap) {
					copyMap = new ConcurrentHashMap<>(mainMap);
				}

				Future<?> saveJsonTask = ioTasks.submit(() -> writeProcessInfoJson(copyMap));
				saveJsonTask.get();

			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				backendTask.shutdown();
				ioTasks.shutdown();
			}
		});
	}

	private void writeProcessInfoJson(ConcurrentHashMap<ProcessKey, ProcessData> mapToWrite) {
		try {
			Path path = Paths.get(mappingFile);

			if (!Files.exists(path)) {
				ProcessInfoJsonUtil.createEmptyJsonFile(path);
			}

			String existingJson = Files.readString(path);
			List<JsonProcessInfo> infos = ProcessInfoJsonUtil.parseJson(existingJson);

			for (ProcessData data : getUniqueProcessesByName(mapToWrite)) {
				ProcessInfoJsonUtil.upsertProcessInfo(infos, data);
			}

			ProcessInfoJsonUtil.writeJson(path, infos);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean sameOriginalProcessName(ProcessData data, String name) {
		if (data == null || data.getName() == null || name == null) {
			return false;
		}

		return data.getName().equalsIgnoreCase(name);
	}

	private List<ProcessData> getUniqueProcessesByName() {
		return getUniqueProcessesByName(mainMap);
	}

	private List<ProcessData> getUniqueProcessesByName(ConcurrentHashMap<ProcessKey, ProcessData> sourceMap) {
		ConcurrentHashMap<String, ProcessData> uniqueByName = new ConcurrentHashMap<>();

		for (ProcessData data : sourceMap.values()) {
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

		List<ProcessData> uniqueProcesses = new ArrayList<>(uniqueByName.values());
		uniqueProcesses.sort(Comparator.comparing(ProcessData::getDisplayName, String.CASE_INSENSITIVE_ORDER));
		return uniqueProcesses;
	}

	// helperi za front

	public boolean isBackendReady() {
		return sc != null && ws != null && am != null && st != null;
	}

	public List<ProcessData> getAllProcesses() {
		List<ProcessData> processes = new ArrayList<>(mainMap.values());
		processes.sort(Comparator.comparing(ProcessData::getDisplayName, String.CASE_INSENSITIVE_ORDER));
		return processes;
	}

	public List<ProcessData> getProcessesByCategory(String category) {
		List<ProcessData> filtered = new ArrayList<>();

		for (ProcessData data : getUniqueProcessesByName()) {
			if (data == null) {
				continue;
			}

			String currentCategory = data.getCategory();

			if (category == null || category.isBlank()) {
				if (currentCategory == null || currentCategory.isBlank()) {
					filtered.add(data);
				}
			} else {
				if (currentCategory != null && currentCategory.equalsIgnoreCase(category)) {
					filtered.add(data);
				}
			}
		}

		filtered.sort(Comparator.comparingLong(ProcessData::getTotalTimeSeconds).reversed());
		return filtered;
	}

	public ProcessData getProcess(ProcessKey key) {
		if (key == null) {
			return null;
		}

		return mainMap.get(key);
	}

	public ProcessKey findProcessKeyByName(String processName) {
		if (processName == null || processName.isBlank()) {
			return null;
		}

		for (ProcessKey key : mainMap.keySet()) {
			ProcessData data = mainMap.get(key);

			if (data == null) {
				continue;
			}

			if (data.getName() != null && data.getName().equalsIgnoreCase(processName)) {
				return key;
			}

			if (data.getAlias() != null && data.getAlias().equalsIgnoreCase(processName)) {
				return key;
			}
		}

		return null;
	}

	public long getWorkTimeSeconds() {
		if (am == null) {
			return 0;
		}
		return am.getWorkTimeSeconds();
	}

	public long getFunTimeSeconds() {
		if (am == null) {
			return 0;
		}
		return am.getFunTimeSeconds();
	}

	public long getOtherTimeSeconds() {
		if (am == null) {
			return 0;
		}
		return am.getOtherTimeSeconds();
	}

	public long getUncategorizedTimeSeconds() {
		if (am == null) {
			return 0;
		}
		return am.getUncategorizedTimeSeconds();
	}


	public String formatSeconds(long totalSeconds) {
		long hours = totalSeconds / 3600;
		long minutes = (totalSeconds % 3600) / 60;
		long seconds = totalSeconds % 60;

		return hours + "h " + minutes + "m " + seconds + "s";
	}

	public String getFormattedWorkTime() {
		return formatSeconds(getWorkTimeSeconds());
	}

	public String getFormattedFunTime() {
		return formatSeconds(getFunTimeSeconds());
	}

	public String getFormattedOtherTime() {
		return formatSeconds(getOtherTimeSeconds());
	}

	public String getFormattedUncategorizedTime() {
		return formatSeconds(getUncategorizedTimeSeconds());
	}
}