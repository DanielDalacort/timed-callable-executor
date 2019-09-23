package ru.pixonic.executor;

import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Example {

    private static final Logger LOGGER = Logger.getLogger(Example.class.getName());

    public static void main(String[] args) throws InterruptedException {
        var backlog = new TaskBacklog<Integer>();
        var executor = new TaskBacklogExecutor<>(backlog);

        executor.start(2500);

        final var now = LocalDateTime.now();

        new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                LOGGER.warning("Interrupted sleeping");
            }

            // Short event from this thread must be executed AFTER short event from other thread
            LOGGER.info("Add short event [+1]");
            backlog.add(now.plusSeconds(1), () -> {
                LOGGER.info("Task: 2nd short event");
                return 1;
            });
            LOGGER.info("Add long event [+0]");
            backlog.add(now.plusSeconds(0), () -> {
                LOGGER.info("Task: Long event started");
                Thread.sleep(5000);
                LOGGER.info("Task: Long event done");
                return 2;
            });
        }).start();

        new Thread(() -> {
            LOGGER.info("Add too far event [+15]");
            backlog.add(now.plusSeconds(15), () -> {
                // this task must not be executed in this example
                assert false;
                return 0;
            });

            // Short event from this thread must be executed BEFORE short event from other thread
            LOGGER.info("Add short event [+1]");
            backlog.add(now.plusSeconds(1), () -> {
                LOGGER.info("Task: 1st short event");
                return 3;
            });
            LOGGER.info("Add exceptional event [+7]");
            backlog.add(now.plusSeconds(7), () -> {
                LOGGER.info("Task: Exceptional event");
                throw new Exception("Expected exception");
            });

        }).start();

        // Lets executor work for 10 seconds
        Thread.sleep(10000);

        LOGGER.info("Results:");
        executor.getResults().forEach((id, v) ->
                LOGGER.info(String.format("Task %s executed successfully with result %s", id, v))
        );

        LOGGER.info("Exceptions:");
        executor.getExceptions().forEach((id, e) ->
                LOGGER.log(Level.WARNING, String.format("Task %s failed with exception:", id), e)
        );
    }

}
