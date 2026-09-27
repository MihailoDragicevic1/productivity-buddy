package src.Backend.main;

import java.time.Instant;
import java.util.Objects;

public class ProcessKey {

    private final long pid;
    private final Instant startTime;

    public ProcessKey(long pid, Instant startTime) {
        this.pid = pid;
        this.startTime = startTime;
    }

    public long getPid() {
        return pid;
    }

    public Instant getStartTime() {
        return startTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProcessKey other)) return false;
        return pid == other.pid && Objects.equals(startTime, other.startTime);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pid, startTime);
    }

    @Override
    public String toString() {
        return "ProcessKey{" +
                "pid=" + pid +
                ", startTime=" + startTime +
                '}';
    }
}