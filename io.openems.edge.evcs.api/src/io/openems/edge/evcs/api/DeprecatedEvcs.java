package io.openems.edge.evcs.api;

import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * This interface marks old implementations of {@link Evcs} that did not yet
 * inherit {@link ElectricityMeter}.
 * 
 * <p>
 * It should not be used for new implementations, but serves as a migration path
 * for old implementations.
 */
public interface DeprecatedEvcs extends ElectricityMeter, OpenemsComponent {

	/**
	 * Copies values to Deprecated Channels during migration to
	 * {@link ElectricityMeter}.
	 * 
	 * <ul>
	 * <li>ACTIVE_POWER -> CHARGE_POWER
	 * <li>ACTIVE_PRODUCTION_ENERGY -> ACTIVE_CONSUMPTION_ENERGY
	 * </ul>
	 * 
	 * @param meter instance of myself
	 */
	public static void copyToDeprecatedEvcsChannels(DeprecatedEvcs meter) {
		var chargePowerChannel = meter.<IntegerReadChannel>channel(DeprecatedEvcs.ChannelId.CHARGE_POWER);
		meter.getActivePowerChannel().onSetNextValue(v -> chargePowerChannel.setNextValue(v.get()));
		meter.getActiveProductionEnergyChannel().onSetNextValue(v -> meter._setActiveConsumptionEnergy(v.get()));
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Copy of {@link ElectricityMeter.ChannelId#ACTIVE_POWER}.
		 */
		CHARGE_POWER(Doc.of(INTEGER) //
				.unit(WATT) //
				.persistencePriority(HIGH));

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
