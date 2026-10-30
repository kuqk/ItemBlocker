package pl.variant.itemblocker.testing;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A logger that keeps its warnings so tests can assert on them.
 */
public final class RecordingLogger {

    private final Logger logger;
    private final List<String> warnings = new ArrayList<>();

    public RecordingLogger() {
        logger = Logger.getAnonymousLogger();
        logger.setUseParentHandlers(false);
        logger.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                if (record.getLevel().intValue() >= Level.WARNING.intValue()) {
                    warnings.add(record.getMessage());
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        });
    }

    public Logger logger() {
        return logger;
    }

    public List<String> warnings() {
        return warnings;
    }
}
