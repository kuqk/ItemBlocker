package pl.variant.itemblocker.rule;

/**
 * How much of an item a player may have: the largest stack and the most in their inventory. Zero
 * means no limit.
 *
 * @param stack     largest stack size, or 0
 * @param inventory most items of this type in a player's inventory, or 0
 */
public record ItemLimit(int stack, int inventory) {

    public static final ItemLimit NONE = new ItemLimit(0, 0);

    public ItemLimit {
        if (stack < 0 || inventory < 0) {
            throw new IllegalArgumentException("Limits cannot be negative");
        }
    }

    public boolean isNone() {
        return stack == 0 && inventory == 0;
    }

    public boolean limitsStack() {
        return stack > 0;
    }

    public boolean limitsInventory() {
        return inventory > 0;
    }

    public ItemLimit withStack(int newStack) {
        return new ItemLimit(newStack, inventory);
    }

    public ItemLimit withInventory(int newInventory) {
        return new ItemLimit(stack, newInventory);
    }
}
