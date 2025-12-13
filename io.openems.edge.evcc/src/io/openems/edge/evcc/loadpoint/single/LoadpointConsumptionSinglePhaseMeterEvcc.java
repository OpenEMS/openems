package io.openems.edge.evcc.loadpoint.single;

import static io.openems.common.types.OpenemsType.DOUBLE;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.SinglePhaseMeter;

/**
 * Interface representing the evcc Loadpoint consumption meter component.
 *
 * <p>
 * Provides channels for measuring electrical properties such as power, voltage,
 * current, frequency, and energy consumption.
 * </p>
 */
public interface LoadpointConsumptionSinglePhaseMeterEvcc extends SinglePhaseMeter, ElectricityMeter, OpenemsComponent {

	/**
	 * Enum for channel identifiers used in this meter.
	 */
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		CONSUMPTION_ENERGY(Doc.of(DOUBLE) //
				.unit(Unit.KILOWATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		ACTIVE_SESSION_ENERGY(Doc.of(DOUBLE) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		ACTIVE_PHASES(Doc.of(INTEGER) //
				.persistencePriority(PersistencePriority.MEDIUM)) //
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
}