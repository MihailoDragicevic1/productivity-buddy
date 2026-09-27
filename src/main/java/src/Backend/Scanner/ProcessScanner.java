package src.Backend.Scanner;

import src.Backend.main.ProcessData;
import oshi.SystemInfo;
import oshi.software.os.OSProcess;
import oshi.software.os.OperatingSystem;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.RecursiveTask;

public class ProcessScanner extends RecursiveTask<List<ProcessData>> {

	private static final int LIMIT = 10;

	private static final SystemInfo SYSTEM_INFO = new SystemInfo();
	private static final OperatingSystem OS = SYSTEM_INFO.getOperatingSystem();

	private final List<ProcessHandle> procList;
	private final int lowerBound;
	private final int upperBound;
	private final Set<String> blacklist;

	public ProcessScanner(List<ProcessHandle> processes, int lb, int ub,Set<String> blacklist) {
		this.procList = processes;
		this.lowerBound = lb;
		this.upperBound = ub;
		this.blacklist = blacklist;
	}

	private List<ProcessData> work() {
		List<ProcessData> results = new ArrayList<>();

		for (int i = lowerBound; i < upperBound; i++) {
			try {
				ProcessHandle ph = procList.get(i);

				if (ph == null || !ph.isAlive()) {
					continue;
				}

				ProcessHandle.Info info = ph.info();

				Optional<Instant> optionalStart = info.startInstant();
				if (optionalStart.isEmpty()) {
					continue;
				}

				Instant startTime = optionalStart.get();
				Instant now = Instant.now();

				String fullName = info.command().orElse("Unknown");
				String processName = extractProcessName(fullName);


					if (blacklist.contains(processName)) {
						continue;
					}


				Duration uptime = Duration.between(startTime, now);
				if (uptime.isNegative()) {
					uptime = Duration.ZERO;
				}

				long ramUsage = 0L;
				long upTimeMillis = 0L;
				long kernelTime = 0L;
				long userTime = 0L;

				OSProcess osProcess = OS.getProcess((int) ph.pid());
				if (osProcess != null) {
					ramUsage = osProcess.getResidentSetSize();
					upTimeMillis = osProcess.getUpTime();
					kernelTime = osProcess.getKernelTime();
					userTime = osProcess.getUserTime();
				}

				ProcessData data = new ProcessData(
						ph.pid(),
						processName,
						0.0,
						ramUsage,
						uptime,
						ph.isAlive(),
						processName,
						"uncategorized",
						now,
						startTime,
						ph
				);

				data.setUpTimeMillis(upTimeMillis);
				data.setKernelTime(kernelTime);
				data.setUserTime(userTime);

				results.add(data);

			} catch (Exception e) {
				//procs handle
			}
		}

		return results;
	}

	private String extractProcessName(String fullCommand) {
		if (fullCommand == null || fullCommand.isBlank()) {
			return "Unknown";
		}

		fullCommand = fullCommand.replace("\\", "/");
		int lastSlash = fullCommand.lastIndexOf('/');

		if (lastSlash >= 0 && lastSlash < fullCommand.length() - 1) {
			return fullCommand.substring(lastSlash + 1);
		}

		return fullCommand;
	}

	private List<ProcessData> combine(List<ProcessData> leftDatas, List<ProcessData> rightDatas) {
		List<ProcessData> combinedList = new ArrayList<>(leftDatas);
		combinedList.addAll(rightDatas);
		return combinedList;
	}

	@Override
	protected List<ProcessData> compute() {
		if (upperBound - lowerBound <= LIMIT) {
			return work();
		}

		int mid = lowerBound + (upperBound - lowerBound) / 2;

		ProcessScanner leftScanner = new ProcessScanner(procList, lowerBound, mid,blacklist);
		ProcessScanner rightScanner = new ProcessScanner(procList, mid, upperBound,blacklist);

		leftScanner.fork();
		List<ProcessData> rightDatas = rightScanner.compute();
		List<ProcessData> leftDatas = leftScanner.join();

		return combine(leftDatas, rightDatas);
	}
}