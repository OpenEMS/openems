package io.openems.edge.evse.chargepoint.alpitronic;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.evse.chargepoint.alpitronic.common.Alpitronic;
import io.openems.edge.evse.chargepoint.alpitronic.enums.AvailableState;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.TimedataProvider;

public abstract class EvseAlpitronic extends AbstractOpenemsModbusComponent
		implements OpenemsComponent, Alpitronic, ElectricityMeter, TimedataProvider {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		AVAILABLE_STATE(Doc.of(AvailableState.values()));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public Channel<AvailableState> getAvailableStateChannel() {
		return this.channel(ChannelId.AVAILABLE_STATE);
	}

	protected EvseAlpitronic(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Is EVSE ready-for-charging?.
	 * 
	 * @return true/false
	 */
	public abstract boolean getIsReadyForCharging();

	/**
	 * Smallest allowed ChargePower.
	 * 
	 * @return the ChargePower
	 */
	public abstract int getMinChargePower();

	/**
	 * Largest allowed ChargePower.
	 * 
	 * @return the ChargePower
	 */
	public abstract int getMaxChargePower();
}
