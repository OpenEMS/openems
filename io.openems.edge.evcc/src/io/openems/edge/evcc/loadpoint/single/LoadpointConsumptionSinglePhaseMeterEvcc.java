package io.openems.edge.evcc.loadpoint.single;

import static io.openems.common.types.OpenemsType.DOUBLE;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcc.loadpoint.PlugState;
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
		ACTIVE_SESSION_ENERGY(Doc.of(DOUBLE) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.HIGH)),

		CHARGE_TOTAL_IMPORT(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS) //
				.text("Total energy imported from EVCC charger meter") //
				.persistencePriority(PersistencePriority.HIGH)),

		ACTIVE_PHASES(Doc.of(INTEGER) //
				.persistencePriority(PersistencePriority.MEDIUM)),

		/**
		 * Name of the connected vehicle.
		 */
		VEHICLE_NAME(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * EVCC charging mode (pv, now, minpv, off).
		 */
		MODE(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Whether charging is enabled by EVCC.
		 */
		ENABLED(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Cable/Plug state.
		 *
		 * <p>
		 * Generic plug state based on EVCC's boolean "connected" status.
		 */
		PLUG(Doc.of(PlugState.values()) //
				.persistencePriority(PersistencePriority.HIGH)) //
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