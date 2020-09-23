package io.openems.edge.controller.asymmetric.activepowervoltagecharacteristic;

import java.time.Clock;
import java.time.LocalDateTime;

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

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.asymmetric.powercharacteristic.AbstractPowerCharacteristic;
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
public class ActivePowerVoltageCharacteristic extends AbstractPowerCharacteristic
		implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ActivePowerVoltageCharacteristic.class);

	private static final int WAIT_FOR_HYSTERESIS = 20;

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

	@Override
	public void run() throws OpenemsNamedException {
		AsymmetricMeter gridMeter = this.componentManager.getComponent(this.config.meter_id());
		Channel<Integer> gridLineVoltage;
		gridLineVoltage = gridMeter.channel(AsymmetricMeter.ChannelId.VOLTAGE_L1);
		this.voltageRatio = gridLineVoltage.value().orElse(0) / this.nominalVoltage;
		int calculatedPower = 0;
		this.channel(ChannelId.VOLTAGE_RATIO).setNextValue(this.voltageRatio);
		if (this.voltageRatio == 0) {
			log.info("Voltage Ratio is 0");
			return;
		}
		this.voltagePowerMap = this.initialize(config.powerVoltConfig());
		float linePowerValue = this.getValueOfLine(this.voltagePowerMap, this.voltageRatio);

		if (this.lastSetPowerTime.isAfter(LocalDateTime.now(this.clock).minusSeconds(WAIT_FOR_HYSTERESIS))) {
			return;
		}
		lastSetPowerTime = LocalDateTime.now(this.clock);

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
