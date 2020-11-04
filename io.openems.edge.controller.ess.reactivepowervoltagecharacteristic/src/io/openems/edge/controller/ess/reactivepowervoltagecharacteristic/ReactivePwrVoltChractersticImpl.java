package io.openems.edge.controller.ess.reactivepowervoltagecharacteristic;

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
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.linecharacteristic.PolyLine;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.ReactivePowerVoltageCharacteristic", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ReactivePwrVoltChractersticImpl extends AbstractOpenemsComponent
		implements PolyLine, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ReactivePwrVoltChractersticImpl.class);

	private LocalDateTime lastSetPowerTime = LocalDateTime.MIN;

	private float referencePoint;// Voltage Ratio
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
		VOLTAGE_RATIO(Doc.of(OpenemsType.DOUBLE))//
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

	public ReactivePwrVoltChractersticImpl() {
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

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		Channel<Integer> gridLineVoltage = this.meter.channel(SymmetricMeter.ChannelId.VOLTAGE);
		this.referencePoint = gridLineVoltage.value().orElse(0) / (this.config.nominalVoltage() * 1000);
		this.channel(ChannelId.VOLTAGE_RATIO).setNextValue(this.referencePoint);
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

		Value<Integer> apparentPower = this.ess.getMaxApparentPower();
		if (!apparentPower.isDefined() || apparentPower.get() == 0) {
			return;
		}

		// Current version has inverse behaviour of Active Power Voltage Characteristic
		// Charges with Reactive Power (in Active was discharging for
		// lower voltage[compare to nominal voltage])
		Integer power = this.getLineValue(JsonUtils.getAsJsonArray(//
				JsonUtils.parse(this.config.lineConfig())), this.referencePoint).intValue();
		Integer setPower = (int) (apparentPower.orElse(0) * power * 0.01);
		int calculatedPower = this.ess.getPower().fitValueIntoMinMaxPower(this.id(), this.ess, Phase.ALL, Pwr.REACTIVE,
				setPower);
		this.channel(ChannelId.CALCULATED_POWER).setNextValue(calculatedPower);
		this.ess.setReactivePowerEquals(calculatedPower);
	}

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
