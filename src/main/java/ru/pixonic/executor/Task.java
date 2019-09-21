package ru.pixonic.executor;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.concurrent.Callable;

@Data
@AllArgsConstructor
public class Task<T> implements Comparable<Task> {

    private String id;
    private Long serial;
    private LocalDateTime time;
    private Callable<T> callable;

    @Override
    public int compareTo(Task task) {
        int byTime = this.getTime().compareTo(task.getTime());
        if (byTime == 0) {
            return this.getSerial().compareTo(task.getSerial());
        }
        return byTime;
    }
}
