package pl.variant.itemblocker.storage;

import pl.variant.itemblocker.rule.RuleBook;

import java.io.IOException;
import java.util.function.UnaryOperator;

/**
 * Holds the current rules and writes every change to {@code rules.yml}.
 *
 * <p>If the file could not be loaded, changes are refused until a reload succeeds, so a command can
 * never overwrite a file the server owner still has to fix.
 */
public final class RuleRepository {

    private final RuleFile file;
    private volatile RuleBook rules = RuleBook.empty();
    private volatile boolean writable;

    public RuleRepository(RuleFile file) {
        this.file = file;
    }

    public RuleBook rules() {
        return rules;
    }

    /**
     * Loads the rules from disk, creating the file first if it does not exist.
     *
     * @throws IOException if the file cannot be read or parsed; the current rules stay in place
     */
    public synchronized void reload() throws IOException {
        if (!file.exists()) {
            file.save(RuleBook.empty());
        }

        try {
            rules = file.load();
            writable = true;
        } catch (IOException exception) {
            writable = false;
            throw exception;
        }
    }

    /**
     * Applies a change, saves it and returns the new rules.
     *
     * @throws IOException if the rules cannot be saved, or the last load failed
     */
    public synchronized RuleBook update(UnaryOperator<RuleBook> change) throws IOException {
        if (!writable) {
            throw new IOException(file.path().getFileName() + " failed to load, fix it and reload before making changes");
        }

        RuleBook updated = change.apply(rules);
        file.save(updated);
        rules = updated;
        return updated;
    }
}
