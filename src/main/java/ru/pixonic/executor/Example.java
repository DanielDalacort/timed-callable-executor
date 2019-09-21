package ru.pixonic.executor;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

@Slf4j
public class Example {

    public static void main(String[] args) throws InterruptedException {
        var backlog = new TaskBacklog<Integer>();
        var executor = new TaskBacklogExecutor<>(backlog);

        executor.start(2500);

        final var now = LocalDateTime.now();

        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                log.error("Interrupted sleeping");
            }

            // Short event from this thread must be executed AFTER short event from other thread
            log.debug("Add short event [+1]");
            backlog.add(now.plusSeconds(1), () -> {
                log.debug("Task: 2nd short event");
                return 1;
            });
            log.debug("Add long event [+0]");
            backlog.add(now.plusSeconds(0), () -> {
                log.debug("Task: Long event started");
                Thread.sleep(5000);
                log.debug("Task: Long event done");
                return 2;
            });
        }).start();

        new Thread(() -> {
            log.debug("Add too far event [+15]");
            backlog.add(now.plusSeconds(15), () -> {
                // this task must not be executed in this example
                assert false;
                return 0;
            });

            // Short event from this thread must be executed BEFORE short event from other thread
            log.debug("Add short event [+1]");
            backlog.add(now.plusSeconds(1), () -> {
                log.debug("Task: 1st short event");
                return 3;
            });
            log.debug("Add exceptional event [+7]");
            backlog.add(now.plusSeconds(7), () -> {
                log.debug("Task: Exceptional event");
                throw new Exception("Expected exception");
            });

        }).start();

        // Lets executor work for 10 seconds
        Thread.sleep(10000);

        log.debug("Results:");
        executor.getResults().forEach((id, v) ->
                log.debug("Task {} executed successfully with result {}", id, v)
        );

        log.debug("Exceptions:");
        executor.getExceptions().forEach((id, e) ->
                log.error(String.format("Task %s failed with exception:", id), e)
        );
    }

}
