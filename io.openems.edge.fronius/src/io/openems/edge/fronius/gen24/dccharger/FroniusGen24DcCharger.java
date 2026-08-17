package io.openems.edge.fronius.gen24.dccharger;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

public interface FroniusGen24DcCharger extends EssDcCharger, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Inverter Operating State.
		 *
		 * <p>
		 * Mirrors the SunSpec Model S103 {@code St} point - e.g.
		 * Off/Sleeping/Starting/MPPT/Throttled/Shutting Down/Fault/Standby.
		 *
		 * <p>
		 * Note: this is the whole-inverter status, not a per-PV-string status - SunSpec
		 * Model S160's per-module {@code DCSt} point is explicitly marked "not
		 * supported" in Fronius' own official Gen24 register map, so it cannot be used
		 * here. This component reads {@code S103.St} independently (same physical
		 * device the BatteryInverter also reads it from), since this Charger holds no
		 * reference to the BatteryInverter.
		 */
		OPERATING_STATE(Doc.of(DefaultSunSpecModel.S103_St.values())), //
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
	 * Gets the SunSpec Channel S160Module1DCW.
	 *
	 * @return the Channel
	 * @throws OpenemsException if the Channel is not present
	 */
	public Channel<Float> getModule1DcwChannel() throws OpenemsException;

	/**
	 * Gets the SunSpec Channel S160Module2DCW.
	 *
	 * @return the Channel
	 * @throws OpenemsException if the Channel is not present
	 */
	public Channel<Float> getModule2DcwChannel() throws OpenemsException;

	/**
	 * Gets the Channel for {@link ChannelId#OPERATING_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<DefaultSunSpecModel.S103_St> getOperatingStateChannel() {
		return this.channel(ChannelId.OPERATING_STATE);
	}

	/**
	 * Gets the Inverter Operating State. See {@link ChannelId#OPERATING_STATE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<DefaultSunSpecModel.S103_St> getOperatingState() {
		return this.getOperatingStateChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#OPERATING_STATE}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setOperatingState(DefaultSunSpecModel.S103_St value) {
		this.getOperatingStateChannel().setNextValue(value);
	}
}
