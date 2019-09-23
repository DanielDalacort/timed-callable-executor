package ru.pixonic.executor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class TaskBacklogExecutor<T> implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(TaskBacklogExecutor.class.getName());

    /**
     * The maximum time to wait, in milliseconds. Stop the executor when time is up.
     */
    private long waitingTimeout;

    /**
     * Queue of task that will be executed
     */
    private final TaskBacklog<T> backlog;

    /**
     * Main thread that execute tasks
     * TODO: probably I should use thread pool
     */
    private volatile Thread executorThread;

    /**
     * Map of task results
     */
    private Map<String, T> results = new ConcurrentHashMap<>();

    /**
     * Map of exceptions that might be thrown during task execution
     */
    private Map<String, Exception> exceptions = new ConcurrentHashMap<>();

    public TaskBacklogExecutor(TaskBacklog<T> backlog) {
        this.backlog = backlog;
    }

    /**
     * Starts the async backlog processing without waiting timeout.
     */
    public void start() {
        start(0L);
    }

    /**
     * Starts the async backlog processing with specified waiting timeout.
     */
    public synchronized void start(long waitingTimeout) {
        this.waitingTimeout = waitingTimeout;

        executorThread = new Thread(this);
        executorThread.setDaemon(true);
        executorThread.start();
    }

    /**
     * Stops the async backlog processing.
     * https://docs.oracle.com/javase/8/docs/technotes/guides/concurrency/threadPrimitiveDeprecation.html
     */
    public synchronized void stop() {
        executorThread = null;
        backlog.interrupt();
    }

    /**
     * Actually the backlog processing.
     * Execution the tasks from backlog when it's time.
     */
    public void run() {
        var thisThread = Thread.currentThread();
        while (executorThread == thisThread) {
            try {
                Task<T> task = backlog.waitReadyForExecutionTask(waitingTimeout);
                try {
                    results.put(task.getId(), task.getCallable().call());
                } catch (Exception e) {
                    exceptions.put(task.getId(), e);
                }
            } catch (InterruptedException e) {
                stop();
                LOGGER.warning("Waiting tasks from backlog interrupted. Executor stopped");
            }
        }
    }

    /* Asking backlog for ready tasks in a loop
       it's simple but bad way due to to high CPU utilization

    public void run() {
        var thisThread = Thread.currentThread();
        while (executorThread == thisThread) {
            backlog.getReadyForExecutionTask().ifPresent(
                    task -> {
                        try {
                            LOGGER.debug("Start task with requested time {}", task.getTime());
                            results.put(task.getId(), task.getCallable().call());
                        } catch (Exception e) {
                            exceptions.put(task.getId(), e);
                        }
                    }
            );
        }
    }
    */

    /**
     * @return true if executor is started
     */
    public boolean isStarted() {
        return executorThread != null && executorThread.isAlive();
    }

    public Map<String, T> getResults() {
        return results;
    }

    public Map<String, Exception> getExceptions() {
        return exceptions;
    }
}
