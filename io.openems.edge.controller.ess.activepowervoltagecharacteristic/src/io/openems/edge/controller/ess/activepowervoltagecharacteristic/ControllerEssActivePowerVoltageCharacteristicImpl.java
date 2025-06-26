package io.openems.edge.controller.ess.activepowervoltagecharacteristic;

import java.time.Duration;
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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.linecharacteristic.PolyLine;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.ActivePowerVoltageCharacteristic", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE//
)
public class ControllerEssActivePowerVoltageCharacteristicImpl extends AbstractOpenemsComponent
		implements ControllerEssActivePowerVoltageCharacteristic, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerEssActivePowerVoltageCharacteristicImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ElectricityMeter meter;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Reference
	private ComponentManager componentManager;

	private LocalDateTime lastSetPowerTime = LocalDateTime.MIN;
	private Config config;
	private PolyLine pByUCharacteristics = null;

	public ControllerEssActivePowerVoltageCharacteristicImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssActivePowerVoltageCharacteristic.ChannelId.values()//
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", config.meter_id())) {
			return;
		}
		this.config = config;
		this.pByUCharacteristics = new PolyLine("voltageRatio", "power", config.lineConfig());
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
		final Integer power;
		if (this.pByUCharacteristics == null) {
			power = null;
		} else {
			var p = this.pByUCharacteristics.getValue(voltageRatio);
			if (p == null) {
				power = null;
			} else {
				power = p.intValue();
			}
		}
		this._setCalculatedPower(power);

		// Apply Power
		this.ess.setActivePowerEquals(power);
	}
}
