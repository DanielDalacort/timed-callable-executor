package ru.pixonic.executor;

import org.junit.jupiter.api.Test;
import ru.pixonic.executor.Task;
import ru.pixonic.executor.TaskBacklog;
import ru.pixonic.executor.TaskBacklogExecutor;

import java.time.LocalDateTime;
import java.util.List;

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

public class TaskBacklogExecutorTest {

    @Test
    public void orderingTest() {
        var backlog = new TaskBacklog<Integer>();

        // Prepare tasks for backlog
        LocalDateTime now = now();
        backlog.add("0", now.plusSeconds(1), () -> 1);
        backlog.add("1", now.plusSeconds(2), () -> 3);
        backlog.add("2", now.plusSeconds(1), () -> 2);
        backlog.add("3", now.plusSeconds(0), () -> 0);

        // Check the right task order
        assertEquals(
                List.of("3", "0", "2", "1"),
                backlog.getTaskList().stream().map(Task::getId).collect(toList())
        );
    }

    @Test
    public void stopStartTest() throws InterruptedException {
        var backlog = new TaskBacklog<Integer>();
        var executor = new TaskBacklogExecutor<>(backlog);

        backlog.add(now().plusSeconds(0), () -> 0);
        backlog.add(now().plusSeconds(1), () -> 0);

        // Run only for 1 task time
        executor.start();
        Thread.sleep(500);
        executor.stop();

        // 1 task still in backlog, 1 task completed
        assertEquals(1, backlog.size());
        assertEquals(1, executor.getResults().size());

        // Executor stopped, but we will wait some time to check that it really doesnt execute something
        Thread.sleep(2000);

        // Backlog still contain 1 task, result didn't change
        assertEquals(1, backlog.size());
        assertEquals(1, executor.getResults().size());

        // Run for remaining task
        executor.start();
        Thread.sleep(500);
        executor.stop();

        // Backlog is empty, 2 task completed
        assertEquals(0, backlog.size());
        assertEquals(2, executor.getResults().size());

        // No exceptions
        assertEquals(0, executor.getExceptions().size());
    }

    @Test
    public void timeoutStopTest() throws InterruptedException {
        var backlog = new TaskBacklog<Integer>();
        var executor = new TaskBacklogExecutor<>(backlog);

        executor.start(100);
        Thread.sleep(500);
        // Stop executor if there is no tasks for too long
        assertFalse(executor.isStarted());

        backlog.add(now().plusSeconds(10), () -> 1);
        executor.start(100);
        Thread.sleep(500);
        // Stop executor if there is task but it's execution time is too far
        assertFalse(executor.isStarted());
    }

    @Test
    public void backPressureTest() throws InterruptedException {
        var backlog = new TaskBacklog<Integer>();
        var executor = new TaskBacklogExecutor<>(backlog);

        backlog.add(now().plusSeconds(0), () -> {
            Thread.sleep(3000);
            return 0;
        });

        executor.start();
        Thread.sleep(500);

        // add some tasks during execution
        // the execution time will be later than stated in the task since the executor is busy
        backlog.add(now().plusSeconds(1), () -> 1);
        backlog.add(now().plusSeconds(1), () -> 2);

        Thread.sleep(3500);
        executor.stop();

        // No tasks in the backlog
        assertEquals(0, backlog.size());
        // Long task complete and tasks that had to wait also completed
        assertEquals(3, executor.getResults().size());

        // No exceptions
        assertEquals(0, executor.getExceptions().size());
    }

    @Test
    public void reorderingDuringWaitingTest() throws InterruptedException {
        var backlog = new TaskBacklog<Integer>();
        var executor = new TaskBacklogExecutor<>(backlog);

        // task should be executed far in the future
        backlog.add("+10", now().plusSeconds(10), () -> 0);

        executor.start();
        Thread.sleep(500);

        // add task that should be executed right now during backlog processing
        backlog.add("0", now().plusSeconds(0), () -> 1);

        Thread.sleep(500);
        executor.stop();

        // First task still in the backlog, only reordered task completed
        assertEquals(1, backlog.size());
        assertEquals(1, executor.getResults().size());
        assertNotNull(executor.getResults().get("0"));

        // No exceptions
        assertEquals(0, executor.getExceptions().size());
    }

    @Test
    public void getExceptionsTest() throws InterruptedException {
        var backlog = new TaskBacklog<Integer>();
        var executor = new TaskBacklogExecutor<>(backlog);

        // task should be executed far in the future
        backlog.add(now().plusSeconds(0), () -> 0);
        backlog.add("ex", now().plusSeconds(0), () -> {
            throw new Exception("test");
        });
        backlog.add(now().plusSeconds(0), () -> 2);

        executor.start();
        Thread.sleep(500);
        executor.stop();

        // no tasks in the backlog, 2 tasks completed successfully
        assertEquals(0, backlog.size());
        assertEquals(2, executor.getResults().size());

        // one expected exception
        assertEquals(1, executor.getExceptions().size());
        assertEquals("test", executor.getExceptions().get("ex").getMessage());
    }

}
