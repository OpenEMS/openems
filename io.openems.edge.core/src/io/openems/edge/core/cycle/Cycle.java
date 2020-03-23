package io.openems.edge.core.cycle;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public interface Cycle {

	public static final int DEFAULT_CYCLE_TIME = 1000; // in [ms]

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Actual, measured Cycle-Time in [ms].
		 * 
		 * <ul>
		 * <li>Interface: Cycle
		 * <li>Type: Integer
		 * </ul>
		 */
		MEASURED_CYCLE_TIME(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLISECONDS)),
		/**
		 * A configured Controller is not executed because it is disabled.
		 * 
		 * <ul>
		 * <li>Interface: Cycle
		 * <li>Type: State
		 * </ul>
		 */
		IGNORE_DISABLED_CONTROLLER(Doc.of(Level.INFO));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the duration of one global OpenEMS Cycle in [ms].
	 * 
	 * @return the duration in milliseconds
	 */
	public int getCycleTime();

}
