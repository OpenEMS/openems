package io.openems.edge.sungrow.meter;

import static io.openems.edge.meter.api.ElectricityMeter.calculatePhasesFromActivePower;
import static io.openems.edge.meter.api.ElectricityMeter.calculateSumCurrentFromPhases;
import static io.openems.edge.meter.api.ElectricityMeter.calculateAverageVoltageFromPhases;
import static io.openems.edge.meter.api.ElectricityMeter.calculateCurrentsFromActivePowerAndVoltage;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.sungrow.ess.EssSungrow;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Sungrow.Gridmeter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SungrowGridMeterImpl extends AbstractOpenemsComponent
		implements SungrowGridMeter, ElectricityMeter, OpenemsComponent {

	protected Config config = null;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private EssSungrow ess;

	public SungrowGridMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SungrowGridMeter.ChannelId.values(), //
				ElectricityMeter.ChannelId.values() //
		);

		// Provide phase powers
		calculatePhasesFromActivePower(this);

		// Provide missing voltages
		calculateAverageVoltageFromPhases(this);

		// Provide Currents from Power and Voltage
		calculateSumCurrentFromPhases(this);
		calculateCurrentsFromActivePowerAndVoltage(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException, OpenemsNamedException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
		
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}

		this.mapChannelValues();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void mapChannelValues() throws OpenemsException {
		this.ess.getExportPowerChannel().onSetNextValue(newValue -> {
			this._setActivePower(newValue.isDefined() ? -newValue.get() : null);
		});
		this.ess.getTotalExportEnergyChannel().onSetNextValue(newValue -> {
			this._setActiveConsumptionEnergy(newValue.get());
		});
		this.ess.getTotalImportEnergyChannel().onSetNextValue(newValue -> {
			this._setActiveProductionEnergy(newValue.get());
		});
		this.ess.getGridFrequencyChannel().onSetNextValue(newValue -> {
			this._setFrequency(newValue.get());
		});
		this.ess.getVoltageL1Channel().onSetNextValue(newValue -> {
			this._setVoltageL1(newValue.get());
		});
		this.ess.getVoltageL2Channel().onSetNextValue(newValue -> {
			this._setVoltageL2(newValue.get());
		});
		this.ess.getVoltageL3Channel().onSetNextValue(newValue -> {
			this._setVoltageL3(newValue.get());
		});
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

}