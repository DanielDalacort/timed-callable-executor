package ru.pixonic.executor;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;

public class Task<T> implements Comparable<Task> {

    private String id;
    private Long serial;
    private LocalDateTime time;
    private Callable<T> callable;

    public Task(String id, Long serial, LocalDateTime time, Callable<T> callable) {
        this.id = id;
        this.serial = serial;
        this.time = time;
        this.callable = callable;
    }

    public String getId() {
        return id;
    }

    public Long getSerial() {
        return serial;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public Callable<T> getCallable() {
        return callable;
    }

    @Override
    public int compareTo(Task task) {
        int byTime = this.getTime().compareTo(task.getTime());
        if (byTime == 0) {
            return this.getSerial().compareTo(task.getSerial());
        }
        return byTime;
    }
}
