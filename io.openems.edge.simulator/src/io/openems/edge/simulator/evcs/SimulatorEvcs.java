package io.openems.edge.simulator.evcs;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

public interface SimulatorEvcs
		extends SymmetricMeter, AsymmetricMeter, ManagedEvcs, Evcs, OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SIMULATED_CHARGE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	@Override
	public default Value<Long> getActiveConsumptionEnergy() {
		return this.getActiveConsumptionEnergyChannel().getNextValue();
	}

	@Override
	public default void _setActiveConsumptionEnergy(long value) {
		this.channel(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY).setNextValue(value);
	}

	@Override
	public default void _setActiveConsumptionEnergy(Long value) {
		this.channel(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY).setNextValue(value);
	}

	@Override
	public default LongReadChannel getActiveConsumptionEnergyChannel() {
		return this.channel(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY);
	}
}
