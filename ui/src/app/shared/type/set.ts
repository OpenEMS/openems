export class OeSet<T> extends Set<T> {
    /**
     * Adds the value to the set if it does not exist, otherwise if its exists, removes it.
     *
     * @param value The value
     */
    public toggle(value: T): void {
        if (this.has(value)) {
            this.delete(value);
        } else {
            this.add(value);
        }
    }
}
