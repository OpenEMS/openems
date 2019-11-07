package io.openems.edge.controller.symmetric.activepowervoltagecharacteristic;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

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
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.meter.api.AsymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Symmetric.ActivePowerVoltageCharacteristic", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ActivePowerVoltageCharacteristic extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ActivePowerVoltageCharacteristic.class);

//	public static final String GREEN = "\u001B[32m";
	private static final int WAIT_FOR_HYSTERESIS = 20;
	private final Map<Float, Float> voltagePowerMap = new HashMap<>();

	private LocalDateTime lastSetPowerTime = LocalDateTime.MIN;
	private final Clock clock;

	/**
	 * nominal voltage in [mV].
	 */
	private float voltageRatio;
	private float nominalVoltage;
	private Config config;
	private int power = 0;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private AsymmetricMeter meter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private ManagedSymmetricEss ess;

	@Reference
	protected ComponentManager componentManager;


	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		CALCULATED_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT)), //
		PERCENT(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)), //
		VOLTAGE_RATIO(Doc.of(OpenemsType.DOUBLE)), //
		VOLTAGE_RATIOL1(Doc.of(OpenemsType.DOUBLE)), //
		VOLTAGE_RATIOL2(Doc.of(OpenemsType.DOUBLE)), //
		VOLTAGE_RATIOL3(Doc.of(OpenemsType.DOUBLE))//
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

	public ActivePowerVoltageCharacteristic() {
		this(Clock.systemDefaultZone());
	}

	protected ActivePowerVoltageCharacteristic(Clock clock) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
		this.clock = clock;
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "meter", config.meter_id())) {
			return;
		}
		this.config = config;
		this.nominalVoltage = config.nominalVoltage() * 1000;

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
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
	 * 
	 * @throws OpenemsNamedException on error
	 */

	private void initialize(String powerVoltConf) throws OpenemsNamedException {
		try {
			JsonArray jpowerV = JsonUtils.getAsJsonArray(JsonUtils.parse(powerVoltConf));
			for (JsonElement element : jpowerV) {
				Float powerConf = (float) JsonUtils.getAsInt(element, "power");
				float voltageRatioConf = JsonUtils.getAsFloat(element, "voltageRatio");
				this.voltagePowerMap.put(voltageRatioConf, powerConf);
			}
		} catch (NullPointerException e) {
			throw new OpenemsException("Unable to set values [" + powerVoltConf + "] " + e.getMessage());
		}

	}

	/**
	 * Initialize the P by U characteristics.
	 *
	 * <p>
	 * Gets EssID and according to that " componentId / device ", it will "point
	 * out/ indicate/assign" on which power line connected.// Numbers will be
	 * recursive after L3/ess3.See ess4...
	 *
	 * <pre>
	 * [
	 *  { "ess0"	: Ess Cluster}
	 *  { "ess1"	: power line : 1}},
	 *  { "ess2"	: power line : 2}},
	 *  { "ess3"	: power line : 3}},
	 *  { "ess4"	: power line : 1}},
	 *  ......
	 * ]
	 * </pre>
	 * 
	 * @param percentQ the configured Percent-by-Q values
	 * 
	 * @throws OpenemsNamedException on error
	 */
	private essId getEssId() {
		String[] parts = this.config.ess_id().split("ess");
		String part2 = parts[1];
		int essIdInt = Integer.parseInt(part2);

		if (this.config.ess_id().equals("ess0")) {
			return essId.ess0;
		} else if (this.config.ess_id().equals("ess1") || ((essIdInt % 3) == 1)) {
			return essId.ess1;
		} else if (this.config.ess_id().equals("ess2") || ((essIdInt % 3) == 2)) {
			return essId.ess2;
		}
		return essId.ess3;
	}

	private enum essId {
		ess0, ess1, ess2, ess3
	}

	@Override
	public void run() throws OpenemsNamedException {
		AsymmetricMeter gridMeter = this.componentManager.getComponent(this.config.meter_id());
		Channel<Integer> gridLineVoltage;
		switch (this.getEssId()) {
		case ess0:
			this.log.info("\n'ess0' assumed as a essCluster by this controller.\n"
					+ "Each ess number related with power line, ex: ess1 == L1 ");
			return;
		case ess1:
			gridLineVoltage = gridMeter.channel(AsymmetricMeter.ChannelId.VOLTAGE_L1);
			this.voltageRatio = gridLineVoltage.value().orElse(0) / this.nominalVoltage;
			break;
		case ess2:
			gridLineVoltage = gridMeter.channel(AsymmetricMeter.ChannelId.VOLTAGE_L2);
			this.voltageRatio = gridLineVoltage.value().orElse(0) / this.nominalVoltage;
			break;
		case ess3:
			gridLineVoltage = gridMeter.channel(AsymmetricMeter.ChannelId.VOLTAGE_L3);
			this.voltageRatio = gridLineVoltage.value().orElse(0) / this.nominalVoltage;
			break;
		}
		int calculatedPower = 0;
		this.channel(ChannelId.VOLTAGE_RATIO).setNextValue(this.voltageRatio);
		if (this.voltageRatio == 0) {
			log.info("Voltage Ratio is 0");
			return;
		}
		this.initialize(config.powerVoltConfig());
		float linePowerValue = Utils.getValueOfLine(this.voltagePowerMap, this.voltageRatio);

		if (this.lastSetPowerTime.isAfter(LocalDateTime.now(this.clock).minusSeconds(WAIT_FOR_HYSTERESIS))) {
			return;
		}
		lastSetPowerTime = LocalDateTime.now(this.clock);
		System.out.println("========refreshing ============= " + lastSetPowerTime);

		if (linePowerValue == 0) {
			log.info("Voltage in the Safe Zone; Power will not set in power voltage characteristic controller");
			this.channel(ChannelId.CALCULATED_POWER).setNextValue(0);
			return;
		}
		this.power = (int) linePowerValue;
		calculatedPower = ess.getPower().fitValueIntoMinMaxPower(this.id(), ess, Phase.ALL, Pwr.ACTIVE, this.power);
		this.channel(ChannelId.CALCULATED_POWER).setNextValue(calculatedPower);
		this.ess.addPowerConstraintAndValidate("ActivePowerVoltageCharacteristic", Phase.ALL, Pwr.ACTIVE,
				Relationship.EQUALS, calculatedPower);
	}
}
