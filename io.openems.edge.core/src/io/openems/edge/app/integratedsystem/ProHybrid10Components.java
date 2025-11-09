package io.openems.edge.app.integratedsystem;

import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;

public class ProHybrid10Components {

	/**
	 * Crrates a default ess component for a FENECON Pro Hybrid.
	 * 
	 * @param essId the id of the ess
	 * @return the {@link Component}
	 */
	public static Component ess(final String essId) {
		return new Component(essId, essId, "Kaco.BlueplanetHybrid10.Ess", JsonUtils.buildJsonObject() //
				.addProperty("enabled", true) //
				.addProperty("capacity", 10200) //
				.addProperty("core.id", "kacoCore0") //
				.addProperty("readOnly", true) //
				.build());
	}

	/**
	 * Creates a default grid meter component for a FENECON Pro Hybrid.
	 *
	 * @param meterId the id of the grid meter
	 * @return the {@link Component}
	 */
	public static Component gridMeter(final String meterId) {
		return new Component(meterId, meterId, "Kaco.BlueplanetHybrid10.GridMeter", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("core.id", "kacoCore0") //
						.addProperty("external", false) //
						.build());
	}

	/**
	 * Creates a default charger component for a FENECON Pro Hybrid.
	 * 
	 * @param chargerId the id of the charger
	 * @return the {@link Component}
	 */
	public static Component charger(final String chargerId) {
		return new Component(chargerId, chargerId, "Kaco.BlueplanetHybrid10.Charger", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addProperty("core.id", "kacoCore0") //
						.build());
	}

	/**
	 * Creates a default kaco core component for a FENECON Pro Hybrid.
	 * 
	 * @param kacoCoreId   the id of the kaco core
	 * @param serialNumber the serial number of kaco core
	 * @param ip           the ip of kaco core
	 * @param userkey      the userkey of kaco core
	 * @return the {@link Component}
	 */
	public static Component kacoCore(final String kacoCoreId, String serialNumber, String ip, String userkey) {

		return new Component(kacoCoreId, kacoCoreId, "Kaco.BlueplanetHybrid10.Core", //
				JsonUtils.buildJsonObject() //
						.addProperty("enabled", true) //
						.addPropertyIfNotNull("ip", ip) //
						.addPropertyIfNotNull("serialnumber", serialNumber) //
						.onlyIf(userkey != null && !userkey.equals("xxx"), b -> {
							b.addProperty("userkey", userkey);
						}) //
						.build());
	}
}
