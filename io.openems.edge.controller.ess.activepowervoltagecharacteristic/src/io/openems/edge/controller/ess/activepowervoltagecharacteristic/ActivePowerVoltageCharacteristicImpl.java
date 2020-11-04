package io.openems.edge.controller.ess.activepowervoltagecharacteristic;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.linecharacteristic.PolyLine;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.ActivePowerVoltageCharacteristic", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
public class ActivePowerVoltageCharacteristicImpl extends AbstractOpenemsComponent
		implements PolyLine, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ActivePowerVoltageCharacteristicImpl.class);

	private LocalDateTime lastSetPowerTime = LocalDateTime.MIN;

	private float referencePoint; // Voltage Ratio
	private Config config;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricMeter meter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private ManagedSymmetricEss ess;

	@Reference
	protected ComponentManager componentManager;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		CALCULATED_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)), //
		PERCENT(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)), //
		VOLTAGE_RATIO(Doc.of(OpenemsType.DOUBLE)), //
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

	public ActivePowerVoltageCharacteristicImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values()//
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", config.meter_id())) {
			return;
		}
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		GridMode gridMode = this.ess.getGridMode();
		if (gridMode.isUndefined()) {
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
		}
		switch (gridMode) {
		case ON_GRID:
		case UNDEFINED:
			break;
		case OFF_GRID:
			return;
		default:
			break;
		}

		Channel<Integer> gridLineVoltage = this.meter.channel(SymmetricMeter.ChannelId.VOLTAGE);
		// Reference point is the voltage ratio which is required in order to find
		// the line value
		this.referencePoint = gridLineVoltage.value().orElse(0) / (this.config.nominalVoltage() * 1000);
		// Store the reference point in the channel voltage_ratio
		this.channel(ChannelId.VOLTAGE_RATIO).setNextValue(this.referencePoint);
		// In case of no meter data, to avoid to get min power value,
		// it has to return here
		if (this.referencePoint == 0) {
			this.log.info("Voltage Ratio is 0");
			return;
		}
		// Do NOT change Set Power If it Does not exceed the hysteresis time
		Clock clock = this.componentManager.getClock();
		if (this.lastSetPowerTime.isAfter(LocalDateTime.now(clock).minusSeconds(this.config.waitForHysteresis()))) {
			return;
		}
		this.lastSetPowerTime = LocalDateTime.now(clock);

		// getLineValue method: Calculates the line value based on the slope between
		// greater and smaller point which referred by referencePoint.
		Integer power = this.getLineValue(JsonUtils.getAsJsonArray(//
				JsonUtils.parse(this.config.lineConfig())), this.referencePoint).intValue();
		this.channel(ChannelId.CALCULATED_POWER).setNextValue(power);
		this.ess.setActivePowerEquals(power);
		this.ess.setReactivePowerEquals(0);
	}

	/**
	 * Parse Line
	 *
	 * <p>
	 * Pars the given JSON line format to xCoord and yCoord parameters.
	 *
	 * <pre>
	 * [
	 *  { "xCoord": 0.9,         "yCoord":-4000}},
	 *  { "xCoord": 0.93,        "yCoord":-1000}},
	 *  { "xCoord": 1.07,        "yCoord":1000}},
	 *  { "xCoord": 1.1,         "yCoord":4000}}
	 * ]
	 * </pre>
	 *
	 * @param lineConfig the configured x and y coordinates values
	 * @return lineMap
	 * @throws OpenemsNamedException on error
	 */
	@Override
	public TreeMap<Float, Float> parseLine(JsonArray lineConfig) throws OpenemsNamedException {
		TreeMap<Float, Float> lineMap = new TreeMap<>();
		for (JsonElement element : lineConfig) {
			Float xCoordValue = JsonUtils.getAsFloat(element, "xCoord");
			Float yCoordValue = JsonUtils.getAsFloat(element, "yCoord");
			lineMap.put(xCoordValue, yCoordValue);
		}
		return lineMap;
	}

	/**
	 * Get Line Value
	 *
	 * <p>
	 * getLineValue method: Calculates the line value based on the slope between
	 * greater and smaller point which referred by referencePoint.
	 *
	 * @param lineConfig     the configured x and y coordinates values
	 * @param referencePoint indicates the point of the value to be used.
	 * @return (m * referencePoint + t) equals to indicated point value
	 * @throws OpenemsNamedException on error
	 */
	@Override
	public Float getLineValue(JsonArray lineConfig, float referencePoint) throws OpenemsNamedException {
		TreeMap<Float, Float> lineMap = this.parseLine(lineConfig);
		Entry<Float, Float> floorEntry = lineMap.floorEntry(referencePoint);
		Entry<Float, Float> ceilingEntry = lineMap.ceilingEntry(referencePoint);
		// In case of referencePoint is smaller than floorEntry key
		try {
			if (floorEntry.getKey().equals(referencePoint)) {
				return floorEntry.getValue().floatValue();
			}
		} catch (NullPointerException e) {
			return ceilingEntry.getValue().floatValue();
		}
		// In case of referencePoint is bigger than ceilingEntry key
		try {
			if (ceilingEntry.getKey().equals(referencePoint)) {
				return ceilingEntry.getValue().floatValue();
			}
		} catch (NullPointerException e) {
			return floorEntry.getValue().floatValue();
		}

		Float m = (ceilingEntry.getValue() - floorEntry.getValue()) / (ceilingEntry.getKey() - floorEntry.getKey());
		Float t = floorEntry.getValue() - m * floorEntry.getKey();
		return m * referencePoint + t;
	}
}
