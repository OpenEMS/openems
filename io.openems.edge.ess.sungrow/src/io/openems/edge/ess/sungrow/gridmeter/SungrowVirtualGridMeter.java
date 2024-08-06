package io.openems.edge.ess.sungrow.gridmeter;

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
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.sungrow.SungrowEss;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.MeterType;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.Sungrow.Gridmeter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SungrowVirtualGridMeter extends AbstractOpenemsComponent implements ElectricityMeter, OpenemsComponent {

	protected Config config = null;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SungrowEss ess;

	public SungrowVirtualGridMeter() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException, OpenemsNamedException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		if (!this.config.enabled()) {
			return;
		}

		this.mapChannelValues();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void mapChannelValues() throws OpenemsException {
		this.ess.getExportPowerChannel().onUpdate(newValue -> {
			if (newValue.isDefined()) {
				this._setActivePower(-newValue.get());
			}
		});
		this.ess.getTotalExportEnergyChannel().onUpdate(newValue -> {
			if (newValue.isDefined()) {
				this._setActiveConsumptionEnergy(newValue.get());
			}
		});
		this.ess.getTotalImportEnergyChannel().onUpdate(newValue -> {
			if (newValue.isDefined()) {
				this._setActiveProductionEnergy(newValue.get());
			}
		});
		this.ess.getGridFrequencyChannel().onUpdate(newValue -> {
			if (newValue.isDefined()) {
				this._setFrequency(newValue.get());
			}
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
