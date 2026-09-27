package src.Backend.main;

import java.time.Duration;
import java.time.Instant;

public class ProcessData {

	public long pid;

	public String name;
	public double cpuUsage;
	public long ramUsage;

	public Duration uptime;
	public boolean alive;
	public String alias;
	public String category;
	public Instant lastUpdated;
	public Instant startTime;
	public transient ProcessHandle ph;
	public boolean isTrackingFrozen;
	public long totalTimeSeconds;

	private long upTimeMillis;
	private long kernelTime;
	private long userTime;

	public ProcessData(long pid,
					   String name,
					   double cpuUsage,
					   long ramUsage,
					   Duration uptime,
					   boolean alive,
					   String alias,
					   String category,
					   Instant lastUpdated,
					   Instant startTime,
					   ProcessHandle ph) {
		this.pid = pid;
		this.name = name;
		this.cpuUsage = cpuUsage;
		this.ramUsage = ramUsage;
		this.uptime = uptime;
		this.alive = alive;
		this.alias = alias;
		this.category = category;
		this.lastUpdated = lastUpdated;
		this.startTime = startTime;
		this.ph = ph;
		this.isTrackingFrozen = false;
		this.totalTimeSeconds = 0;
		this.upTimeMillis = 0L;
		this.kernelTime = 0L;
		this.userTime = 0L;
	}

	public long getPid() {
		return pid;
	}

	public void setPid(long pid) {
		this.pid = pid;
	}

	public String getName() {
		return name;
	}

	public String getDisplayName() {
		if (alias != null && !alias.isBlank()) {
			return alias;
		}
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public double getCpuUsage() {
		return cpuUsage;
	}

	public void setCpuUsage(double cpuUsage) {
		this.cpuUsage = cpuUsage;
	}

	public long getRamUsage() {
		return ramUsage;
	}

	public void setRamUsage(long ramUsage) {
		this.ramUsage = ramUsage;
	}

	public Duration getUptime() {
		return uptime;
	}

	public void setUptime(Duration uptime) {
		this.uptime = uptime;
	}

	public boolean isAlive() {
		return alive;
	}

	public void setAlive(boolean alive) {
		this.alive = alive;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public Instant getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Instant lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public Instant getStartTime() {
		return startTime;
	}

	public void setStartTime(Instant startTime) {
		this.startTime = startTime;
	}

	public ProcessHandle getPh() {
		return ph;
	}

	public void setPh(ProcessHandle ph) {
		this.ph = ph;
	}

	public boolean isTrackingFrozen() {
		return isTrackingFrozen;
	}

	public void setTrackingFrozen(boolean trackingFrozen) {
		isTrackingFrozen = trackingFrozen;
	}

	public long getTotalTimeSeconds() {
		return totalTimeSeconds;
	}

	public void setTotalTimeSeconds(long totalTimeSeconds) {
		this.totalTimeSeconds = totalTimeSeconds;
	}

	public long getUpTimeMillis() {
		return upTimeMillis;
	}

	public void setUpTimeMillis(long upTimeMillis) {
		this.upTimeMillis = upTimeMillis;
	}

	public long getKernelTime() {
		return kernelTime;
	}

	public void setKernelTime(long kernelTime) {
		this.kernelTime = kernelTime;
	}

	public long getUserTime() {
		return userTime;
	}

	public void setUserTime(long userTime) {
		this.userTime = userTime;
	}

	public void addTrackedSeconds(long secondsToAdd) {
		if (secondsToAdd > 0) {
			this.totalTimeSeconds += secondsToAdd;
		}
	}

	@Override
	public String toString() {
		return "ProcessData{" +
				"pid=" + pid +
				", name='" + name + '\'' +
				", alias='" + alias + '\'' +
				", category='" + category + '\'' +
				", cpuUsage=" + cpuUsage +
				", ramUsage=" + ramUsage +
				", alive=" + alive +
				", isTrackingFrozen=" + isTrackingFrozen +
				", totalTimeSeconds=" + totalTimeSeconds +
				", startTime=" + startTime +
				'}';
	}
}