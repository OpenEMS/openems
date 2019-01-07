package io.openems.edge.ess.dccharger.api;

import org.osgi.annotation.versioning.ProviderType;
import org.osgi.service.cm.ConfigurationAdmin;

import io.openems.common.types.OpenemsType;
import io.openems.common.utils.IntUtils;
import io.openems.common.utils.IntUtils.Round;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface EssDcCharger extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Maximum Ever Actual Power
		 * 
		 * <ul>
		 * <li>Interface: Ess DC Charger
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive or '0'
		 * <li>Implementation Note: value is automatically derived from ACTUAL_POWER
		 * </ul>
		 */
		MAX_ACTUAL_POWER(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT)), //
		/**
		 * Actual Power
		 * 
		 * <ul>
		 * <li>Interface: Ess DC Charger
		 * <li>Type: Integer
		 * <li>Unit: W
		 * <li>Range: positive
		 * </ul>
		 */
		ACTUAL_POWER(new Doc().type(OpenemsType.INTEGER).unit(Unit.WATT).onInit(channel -> {
			channel.onSetNextValue(value -> {
				/*
				 * Fill Max Actual Power channel
				 */
				if (value.asOptional().isPresent()) {
					int newValue = (int) (Integer) value.get();
					Channel<Integer> maxActualPowerChannel = channel.getComponent().channel(ChannelId.MAX_ACTUAL_POWER);
					int maxActualPower = maxActualPowerChannel.value().orElse(0);
					int maxNextActualPower = maxActualPowerChannel.getNextValue().orElse(0);
					if (newValue > Math.max(maxActualPower, maxNextActualPower)) {
						// avoid getting called too often -> round to 100
						newValue = IntUtils.roundToPrecision(newValue, Round.AWAY_FROM_ZERO, 100);
						maxActualPowerChannel.setNextValue(newValue);
					}
				}
			});
		})),
		/**
		 * Actual Energy
		 * 
		 * <ul>
		 * <li>Interface: Ess Symmetric
		 * <li>Type: Integer
		 * <li>Unit: Wh
		 * </ul>
		 */
		ACTUAL_ENERGY(new Doc() //
				.type(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Actual Power
	 * 
	 * @see EssDcCharger.ChannelId#ACTUAL_POWER
	 * 
	 * @return
	 */
	default Channel<Integer> getActualPower() {
		return this.channel(ChannelId.ACTUAL_POWER);
	}

	/**
	 * Gets the Maximum Ever Actual Power.
	 * 
	 * @return
	 */
	default Channel<Integer> getMaxActualPower() {
		return this.channel(ChannelId.MAX_ACTUAL_POWER);
	}

	/**
	 * Gets the Actual Energy in [Wh].
	 * 
	 * @return
	 */
	default Channel<Integer> getActualEnergy() {
		return this.channel(ChannelId.ACTUAL_ENERGY);
	}

	/**
	 * Internal helper method to handle storing MaxActualPower in config property
	 * 'maxActualPower'
	 * 
	 * @param cm
	 * @param servicePid
	 * @param maxActualPowerConfig
	 */
	default void _initializMaxActualPower(ConfigurationAdmin cm, String servicePid, int maxActualPowerConfig) {
		/*
		 * Update min/max active power channels
		 */
		this.getMaxActualPower().setNextValue(maxActualPowerConfig);

		this.getMaxActualPower().onChange(value -> {
			if (value.get() != maxActualPowerConfig) {
				OpenemsComponent.updateConfigurationProperty(cm, servicePid, "maxActualPower", value.get());
			}
		});
	}
}
