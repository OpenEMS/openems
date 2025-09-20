package io.openems.backend.timedata.timescaledb.internal;

public enum Priority {
	LOW(1, "low"), //
	HIGH(2, "high"), //
	;

	private final int id;
	private final String tableSuffix;

	private Priority(int id, String tableSuffix) {
		this.id = id;
		this.tableSuffix = tableSuffix;
	}

	public String getTableSuffix() {
		return this.tableSuffix;
	}

	public int getId() {
		return this.id;
	}

	/**
	 * Gets the priority with the given id.
	 * 
	 * @param id the id of the priority
	 * @return the priority
	 */
	public static final Priority fromId(int id) {
		for (var priority : Priority.values()) {
			if (priority.id == id) {
				return priority;
			}
		}
		return null;
	}

}
