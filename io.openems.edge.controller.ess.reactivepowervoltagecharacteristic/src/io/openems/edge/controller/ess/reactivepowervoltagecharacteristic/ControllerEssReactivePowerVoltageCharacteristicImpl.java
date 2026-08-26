package io.openems.edge.controller.ess.reactivepowervoltagecharacteristic;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.Duration;
import java.time.LocalDateTime;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.linecharacteristic.PolyLine;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.ReactivePowerVoltageCharacteristic", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences({ "ess", "meter" })
public class ControllerEssReactivePowerVoltageCharacteristicImpl extends AbstractOpenemsComponent
		implements ControllerEssReactivePowerVoltageCharacteristic, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerEssReactivePowerVoltageCharacteristicImpl.class);

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.meter_id})(enabled=true))")
	private ElectricityMeter meter;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = OPTIONAL, //
			target = "(&(id=${config.ess_id})(enabled=true))")
	private ManagedSymmetricEss ess;

	@Reference
	private ComponentManager componentManager;

	private LocalDateTime lastSetPowerTime = LocalDateTime.MIN;
	private Config config;
	private PolyLine qByUCharacteristics = null;

	public ControllerEssReactivePowerVoltageCharacteristicImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssReactivePowerVoltageCharacteristic.ChannelId.values()//
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.qByUCharacteristics = new PolyLine("voltageRatio", "percent", config.lineConfig());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		var gridMode = this.ess.getGridMode();
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

		// Ratio between current voltage and nominal voltage
		final Float voltageRatio;
		var gridVoltage = this.meter.getVoltage();
		if (gridVoltage.isDefined()) {
			voltageRatio = gridVoltage.get() / (this.config.nominalVoltage() * 1000);
		} else {
			voltageRatio = null;
		}
		this._setVoltageRatio(voltageRatio);
		if (voltageRatio == null) {
			return;
		}

		// Do NOT change Set Power If it Does not exceed the hysteresis time
		var clock = this.componentManager.getClock();
		var now = LocalDateTime.now(clock);
		if (Duration.between(this.lastSetPowerTime, now).getSeconds() < this.config.waitForHysteresis()) {
			return;
		}
		this.lastSetPowerTime = now;

		// Get P-by-U value from voltageRatio
		final Integer percent;
		if (this.qByUCharacteristics == null) {
			percent = null;
		} else {
			var p = this.qByUCharacteristics.getValue(voltageRatio);
			if (p == null) {
				percent = null;
			} else {
				percent = p.intValue();
			}
		}

		// Gets required maxApparentPower
		// which is used in calculation of reactive power:
		// Otherwise should not calcula the reactive power and has to return here
		var apparentPower = this.ess.getMaxApparentPower();
		Integer power = (int) (apparentPower.getOrError() * percent * 0.01);
		this._setCalculatedPower(power);

		// Apply Power
		this.ess.setReactivePowerEqualsWithoutFilter(power);
	}

}
