package io.openems.edge.energy.api;

import java.util.function.Consumer;

import io.openems.common.jsonrpc.type.ScheduleUpdate;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * The global Energy Schedule optimizer singleton.
 */
public interface EnergyScheduler extends OpenemsComponent {

	public static final String SINGLETON_SERVICE_PID = "Core.Energy";
	public static final String SINGLETON_COMPONENT_ID = "_energy";

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SIMULATIONS_PER_QUARTER(Doc.of(OpenemsType.INTEGER)), //
		GENERATIONS_PER_QUARTER(Doc.of(OpenemsType.INTEGER)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Subscribes to schedule updates.
	 *
	 * <p>
	 * The given consumer is immediately called with the current
	 * {@link ScheduleUpdate} and subsequently whenever the schedule changes.
	 *
	 * @param consumer the consumer to notify about schedule updates
	 */
	void subscribeSchedule(Consumer<ScheduleUpdate> consumer);

	/**
	 * Unsubscribes from schedule updates.
	 *
	 * <p>
	 * The same consumer instance that was previously passed to
	 * {@link #subscribeSchedule(Consumer)} must be provided.
	 *
	 * @param consumer the previously subscribed consumer
	 */
	void unsubscribeSchedule(Consumer<ScheduleUpdate> consumer);
}
