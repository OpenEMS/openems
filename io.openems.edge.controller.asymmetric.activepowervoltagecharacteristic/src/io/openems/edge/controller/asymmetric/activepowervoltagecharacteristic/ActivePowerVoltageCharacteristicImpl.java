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
import io.openems.edge.common.powercharacteristic.AbstractPowerCharacteristic;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.AsymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Symmetric.ActivePowerVoltageCharacteristic", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ActivePowerVoltageCharacteristicImpl extends AbstractPowerCharacteristic
		implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ActivePowerVoltageCharacteristicImpl.class);

	private LocalDateTime lastSetPowerTime = LocalDateTime.MIN;

	/**
	 * nominal voltage in [mV].
	 */
	private float voltageRatio;
	private Config config;

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

	public ActivePowerVoltageCharacteristicImpl() {
		super();
	}

	public ActivePowerVoltageCharacteristicImpl(Clock clock) {
		super(clock);
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
		}
		
		if (this.lastSetPowerTime.isAfter(LocalDateTime.now(super.clock).minusSeconds(this.config.waitForHysteresis()))) {
			return;
		}
		lastSetPowerTime = LocalDateTime.now(super.clock);
		
		AsymmetricMeter gridMeter = componentManager.getComponent(this.config.meter_id());
		Channel<Integer> gridLineVoltage = gridMeter.channel(AsymmetricMeter.ChannelId.VOLTAGE_L1);

		this.voltageRatio = gridLineVoltage.value().orElse(0) / this.config.nominalVoltage()*1000;
//		this.channel(ChannelId.VOLTAGE_RATIO).setNextValue(this.voltageRatio);
		if (this.voltageRatio == 0) {
			log.info("Voltage Ratio is 0");
			return;
		}
		Integer power = getPowerLine(this.config.powerVoltConfig(), this.voltageRatio);
		if (power == null) {
			return;
		}
//		this.channel(ChannelId.CALCULATED_POWER).setNextValue(power);
		this.ess.setActivePowerEquals(power);
		this.ess.setReactivePowerEquals(0);
	}
}
