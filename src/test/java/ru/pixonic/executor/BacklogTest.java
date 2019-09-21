package ru.pixonic.executor;

import org.junit.jupiter.api.Test;
import ru.pixonic.executor.TaskBacklog;

import static java.time.LocalDateTime.now;
import static org.junit.jupiter.api.Assertions.*;

public class BacklogTest {

    @Test
    public void noTaskInBacklogTest() {
        var backlog = new TaskBacklog<Void>();
        assertTrue(backlog.getReadyForExecutionTask().isEmpty());
    }

    @Test
    public void taskExecutionTimeTooFarTest() {
        var backlog = new TaskBacklog<Void>();
        backlog.add(now().plusSeconds(10), () -> null);
        assertTrue(backlog.getReadyForExecutionTask().isEmpty());
    }

    @Test
    public void taskReadyTest() {
        var backlog = new TaskBacklog<Void>();
        backlog.add(now(), () -> null);
        assertTrue(backlog.getReadyForExecutionTask().isPresent());
    }
    @Test
    public void noTaskInBacklogTimeoutTest() {
        var backlog = new TaskBacklog<Void>();
        assertThrows(InterruptedException.class, () ->
                backlog.waitReadyForExecutionTask(100)
        );
    }

    @Test
    public void taskExecutionTimeTooFarTimeoutTest() {
        var backlog = new TaskBacklog<Void>();
        backlog.add(now().plusSeconds(10), () -> null);
        assertThrows(InterruptedException.class, () ->
                backlog.waitReadyForExecutionTask(100)
        );
    }

    @Test
    public void taskReadyTimeoutTest() throws InterruptedException {
        var backlog = new TaskBacklog<Void>();
        backlog.add(now(), () -> null);
        assertNotNull(backlog.waitReadyForExecutionTask(100));
    }

}
