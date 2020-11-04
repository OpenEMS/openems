package io.openems.edge.controller.ess.reactivepowervoltagecharacteristic;

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
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.function.AbstractRampFunction;
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
public class ReactivePwrVoltChractersticImpl extends AbstractRampFunction implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ReactivePwrVoltChractersticImpl.class);

	private LocalDateTime lastSetPowerTime = LocalDateTime.MIN;

	private float voltageRatio;
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
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(cm, this.servicePid(), "meter", config.meter_id())) {
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
		Channel<Integer> gridLineVoltage = this.meter.channel(SymmetricMeter.ChannelId.VOLTAGE);
		this.voltageRatio = gridLineVoltage.value().orElse(0) / (this.config.nominalVoltage() * 1000);
		this.channel(ChannelId.VOLTAGE_RATIO).setNextValue(this.voltageRatio);
		if (this.voltageRatio == 0) {
			log.info("Voltage Ratio is 0");
			return;
		}

		int calculatedPower = 0;
		Integer power = getLineValue(JsonUtils.getAsJsonArray(//
				JsonUtils.parse(this.config.powerVoltConfig())), this.voltageRatio).intValue();

		// Do NOT change Set Power If it Does not exceed the hysteresis time
		Clock clock = this.componentManager.getClock();
		if (this.lastSetPowerTime.isAfter(LocalDateTime.now(clock).minusSeconds(this.config.waitForHysteresis()))) {
			return;
		}
		lastSetPowerTime = LocalDateTime.now(clock);

		Value<Integer> apparentPower = this.ess.getMaxApparentPower();
		if (!apparentPower.isDefined() || apparentPower.get() == 0) {
			return;
		}

		// Current version has inverse behaviour of Active Power Voltage Characteristic
		// Charges with Reactive Power (in Active was discharging for
		// lower voltage[compare to nominal voltage])
		Integer setPower = (int) (apparentPower.orElse(0) * power * 0.01);
		calculatedPower = ess.getPower().fitValueIntoMinMaxPower(this.id(), ess, Phase.ALL, Pwr.REACTIVE, setPower);
		this.channel(ChannelId.CALCULATED_POWER).setNextValue(calculatedPower);
		this.ess.setReactivePowerEquals(calculatedPower);
	}
}
