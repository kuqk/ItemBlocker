package pl.variant.model;

public final class BlockCheckResult {

    private static final BlockCheckResult ALLOWED = new BlockCheckResult(false, null, null, false);

    private final boolean blocked;
    private final String sourceName;
    private final String reason;
    private final boolean simpleList;

    private BlockCheckResult(boolean blocked, String sourceName, String reason, boolean simpleList) {
        this.blocked = blocked;
        this.sourceName = sourceName;
        this.reason = reason;
        this.simpleList = simpleList;
    }

    public static BlockCheckResult allowed() {
        return ALLOWED;
    }

    public static BlockCheckResult blocked(String sourceName, String reason, boolean simpleList) {
        return new BlockCheckResult(true, sourceName, reason, simpleList);
    }

    public boolean isBlocked() {
        return blocked;
    }

    public String getSourceName() {
        return sourceName;
    }

    public String getReason() {
        return reason;
    }

    public boolean isSimpleList() {
        return simpleList;
    }
}
