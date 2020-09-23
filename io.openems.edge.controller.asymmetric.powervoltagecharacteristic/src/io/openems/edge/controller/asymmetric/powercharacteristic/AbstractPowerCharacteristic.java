package io.openems.edge.controller.asymmetric.powercharacteristic;

import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

public abstract class AbstractPowerCharacteristic extends AbstractOpenemsComponent implements OpenemsComponent {

	public final Clock clock;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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

	protected AbstractPowerCharacteristic() {
		this(Clock.systemDefaultZone());
	}

	protected AbstractPowerCharacteristic(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values()//
		);
		this.clock = clock;
	}

	protected void activate(ComponentContext context, String id, String alias) {
		throw new IllegalArgumentException("Use the other activate method");
	}

	/**
	 * Abstract activator.
	 * 
	 * @param context         the Bundle context
	 * @param id              the Component-ID
	 * @param alias           the Component Alias
	 * @param enabled         is the Component enabled?
	 * @param meterId         the Meter-ID
	 * @param noOfBufferHours the number of buffer hours to make sure the battery
	 *                        still charges full, even on prediction errors
	 */
	protected void activate(ComponentContext context, String id, String alias, boolean enabled) {
		super.activate(context, id, alias, enabled);
	}

	/**
	 * Initialize the P by U characteristics.
	 *
	 * <p>
	 * Parsing JSON then putting the point variables into pByUCharacteristicEquation
	 *
	 * <pre>
	 * [
	 *  { "voltageRatio": 0.9, 		"power" : -4000 }},
	 *  { "voltageRatio": 0.93,		"power": -1000 }},
	 *  { "voltageRatio": 1.07,		"power": 0}},
	 *  { "voltageRatio": 1.1, 		"power": 1000 } }
	 * ]
	 * </pre>
	 * 
	 * @param percentQ the configured Percent-by-Q values
	 * @return
	 * 
	 * @throws OpenemsNamedException on error
	 */

	private Map<Float, Float> initialize(String powerConfig) throws OpenemsNamedException {
		Map<Float, Float> voltagePowerMap = new HashMap<>();
		try {
			JsonArray jpowerV = JsonUtils.getAsJsonArray(JsonUtils.parse(powerConfig));
			for (JsonElement element : jpowerV) {
				Float powerConfValue = (float) JsonUtils.getAsInt(element, "power");
				float voltageRatioConfValue = JsonUtils.getAsFloat(element, "voltageRatio");
				voltagePowerMap.put(voltageRatioConfValue, powerConfValue);
			}
		} catch (NullPointerException e) {
			throw new OpenemsException("Unable to set values [" + powerConfig + "] " + e.getMessage());
		}

		return voltagePowerMap;

	}

	public Integer getPowerLine(String powerConfig, float ratio) throws OpenemsNamedException {

		Map<Float, Float> voltagePowerMap = this.initialize(powerConfig);
		float linePowerValue = Utils.getValueOfLine(voltagePowerMap, ratio);

		return (int) linePowerValue;

	}

}
