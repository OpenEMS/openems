package io.openems.edge.ess.symmetric.readonly.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.converter.StaticConverters;
import io.openems.edge.ess.api.Ess;

@ProviderType
public interface SymmetricEssReadonly extends Ess {

	public final static String POWER_DOC_TEXT = "Negative values for Charge; positive for Discharge";

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Charge Active Power
		 * 
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from negative
		 * ACTIVE_POWER
		 * </ul>
		 */
		CHARGE_ACTIVE_POWER(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Discharge Active Power
		 * 
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from ACTIVE_POWER
		 * </ul>
		 */
		DISCHARGE_ACTIVE_POWER(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Active Power
		 * 
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		ACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.text(POWER_DOC_TEXT) //
				.onInit(channel -> {
					channel.onSetNextValue(value -> {
						// derive DISCHARGE_ACTIVE_POWER and CHARGE_ACTIVE_POWER from ACTIVE_POWER
						Object dischargeValue = StaticConverters.KEEP_POSITIVE.apply(value.get());
						channel.getComponent().channel(ChannelId.DISCHARGE_ACTIVE_POWER).setNextValue(dischargeValue);
						Object chargeValue = StaticConverters.INVERT.andThen(StaticConverters.KEEP_POSITIVE)
								.apply(value.get());
						channel.getComponent().channel(ChannelId.CHARGE_ACTIVE_POWER).setNextValue(chargeValue);
					});
				})), //
		/**
		 * Charge Reactive Power
		 * 
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from negative
		 * REACTIVE_POWER
		 * </ul>
		 */
		CHARGE_REACTIVE_POWER(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Discharge Reactive Power
		 * 
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: only positive values
		 * <li>Implementation Note: value is automatically derived from REACTIVE_POWER
		 * </ul>
		 */
		DISCHARGE_REACTIVE_POWER(new Doc().type(OpenemsType.INTEGER).unit(Unit.VOLT_AMPERE_REACTIVE)), //
		/**
		 * Reactive Power
		 * 
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: var
		 * <li>Range: negative values for Charge; positive for Discharge
		 * </ul>
		 */
		REACTIVE_POWER(new Doc() //
				.type(OpenemsType.INTEGER) //
				.unit(Unit.VOLT_AMPERE_REACTIVE) //
				.text(POWER_DOC_TEXT) //
				.onInit(channel -> {
					channel.onSetNextValue(value -> {
						// derive DISCHARGE_REACTIVE_POWER and CHARGE_REACTIVE_POWER from REACTIVE_POWER
						Object dischargeValue = StaticConverters.KEEP_POSITIVE.apply(value.get());
						channel.getComponent().channel(ChannelId.DISCHARGE_REACTIVE_POWER).setNextValue(dischargeValue);
						Object chargeValue = StaticConverters.INVERT.andThen(StaticConverters.KEEP_POSITIVE)
								.apply(value.get());
						channel.getComponent().channel(ChannelId.CHARGE_REACTIVE_POWER).setNextValue(chargeValue);
					});
				}));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Active Power in [W]. Negative values for Charge; positive for
	 * Discharge
	 * 
	 * @return
	 */
	default Channel<Integer> getActivePower() {
		return this.channel(ChannelId.ACTIVE_POWER);
	}

	/**
	 * Gets the Charge Active Power in [W]. This is derived from negative
	 * 'getActivePower()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getChargeActivePower() {
		return this.channel(ChannelId.CHARGE_ACTIVE_POWER);
	}

	/**
	 * Gets the Discharge Active Power in [W]. This is derived from positive
	 * 'getActivePower()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getDischargeActivePower() {
		return this.channel(ChannelId.DISCHARGE_ACTIVE_POWER);
	}

	/**
	 * Gets the Reactive Power in [var]. Negative values for Charge; positive for
	 * Discharge
	 * 
	 * @return
	 */
	default Channel<Integer> getReactivePower() {
		return this.channel(ChannelId.REACTIVE_POWER);
	}

	/**
	 * Gets the Charge Reactive Power in [var]. This is derived from negative
	 * 'getReactivePower()' values; 0 for positive.
	 * 
	 * @return
	 */
	default Channel<Integer> getChargeReactivePower() {
		return this.channel(ChannelId.CHARGE_REACTIVE_POWER);
	}

	/**
	 * Gets the Discharge Reactive Power in [var]. This is derived from positive
	 * 'getReactivePower()' values; 0 for negative.
	 * 
	 * @return
	 */
	default Channel<Integer> getDischargeReactivePower() {
		return this.channel(ChannelId.DISCHARGE_REACTIVE_POWER);
	}
}
