package ru.pixonic.executor;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.logging.Logger;

import static java.lang.String.format;
import static java.time.LocalDateTime.now;
import static java.util.Optional.empty;
import static java.util.Optional.of;

public class TaskBacklog<T> {

    private static final Logger LOGGER = Logger.getLogger(TaskBacklog.class.getName());

    /**
     * Number of tasks that had been added in backlog for all backlog lifetime
     */
    private long counter = 0;

    /**
     * Queue of task that should be executed in future
     */
    private PriorityBlockingQueue<Task<T>> queue = new PriorityBlockingQueue<>();

    /**
     * The least time of tasks from backlog for execution.
     * Used for optimization notification of {@link #waitReadyForExecutionTask(long)}
     */
    private LocalDateTime leastTime;

    /**
     * Flag that indicates that waiting status the anybody ask for waiting the task.
     * Used for starting and interruption the waiting process
     */
    private volatile boolean waiting = false;

    /**
     * Creates and add {@link Task} to backlog with random UUID
     * @param time  time when the task should be executed
     * @param callable  actually the task that should be executed
     */
    public void add(LocalDateTime time, Callable<T> callable) {
        this.add(UUID.randomUUID().toString(), time, callable);
    }

    /**
     * Creates and add {@link Task} to backlog
     * @param id id of a task
     * @param time time when the task should be executed
     * @param callable actually the task that should be executed
     */
    public synchronized void add(String id, LocalDateTime time, Callable<T> callable) {
        LOGGER.fine(format("BL: Add Task %s in backlog", id));
        queue.add(new Task<>(id, counter++, time, callable));
        if (leastTime == null || time.isBefore(leastTime)) {
            leastTime = time;
            notifyAll();
        }
    }

    /**
     * Peeks tasks from backlog queue and check execution time of task. Pool if its time to execute task.
     * @return Optional of task that must be executed right now.
     *         Empty if queue is empty of there are only tasks that must be executed in future/
     */
    public Optional<Task<T>> getReadyForExecutionTask() {
        var task = queue.peek();
        if (task != null) {
            if (!now().isBefore(task.getTime())) {
                return of(queue.poll());
            }
        }
        return empty();
    }

    /**
     * Waits a task from backlog that must be executed until it is awakened,
     * typically by being notified or interrupted, or until a certain amount time has elapsed.
     * @param  timeoutMillis  the maximum time to wait, in milliseconds
     * @return Task that must be executed right now.
     * @throws InterruptedException if any thread interrupted the current thread before or
     *         while the current thread was waiting.
     */
    public Task<T> waitReadyForExecutionTask(long timeoutMillis) throws InterruptedException {
        if (timeoutMillis > 0) {
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    interrupt();
                }
            }, timeoutMillis);
        }

        waiting = true;
        while (waiting) {
            // Sync whole loop iteration block in order to not poll
            // a new object inserted during processing peeked object
            synchronized (this) {
                var task = queue.peek();
                if (task == null) {
                    LOGGER.fine("BL: Wait for a new task");
                    wait(timeoutMillis);
                } else {
                    long until = now().until(task.getTime(), ChronoUnit.MILLIS);
                    if (until > 0) {
                        LOGGER.fine(format("BL: Wait task %s for %sms", task.getId(), until));
                        wait(timeoutMillis != 0 ? Math.min(until, timeoutMillis) : until);
                    } else {
                        LOGGER.fine(format("BL: Return task %s", task.getId()));
                        return queue.poll();
                    }
                }
            }
        }

        throw new InterruptedException("Task waiting interrupted");
    }

    /**
     * Interrupts the waiting of tasks.
     * Notify the {@link #waitReadyForExecutionTask(long)} if you need to interrupt
     * the waiting of task that must be executed in the future.
     */
    public void interrupt() {
        if (waiting) {
            synchronized (this) {
                waiting = false;
                notifyAll();
            }
        }
    }

    /**
     * Returns the number of elements in queue.
     */
    public int size() {
        return queue.size();
    }

    /**
     * Returns the snapshot list of tasks.
     */
    public List<Task<T>> getTaskList() {
        return new ArrayList<>(queue);
    }

}
