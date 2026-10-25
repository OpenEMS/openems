package io.openems.edge.sonnen.meter;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.types.MeterType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.sonnen.core.SonnenCore;
import io.openems.edge.sonnen.core.SonnenPowermeter;
import io.openems.edge.sonnen.core.SonnenStatus;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Sonnen.Meter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("core")
public class MeterSonnenImpl extends AbstractOpenemsComponent
		implements MeterSonnen, ElectricityMeter, OpenemsComponent {

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, target = "(&(id=${config.core_id})(enabled=true))")
	private SonnenCore core;

	public MeterSonnenImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterSonnen.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (!config.enabled()) {
			return;
		}
		this.core.setMeter(this);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.core != null) {
			this.core.setMeter(null);
		}
		super.deactivate();
	}

	@Override
	public void updateFromStatus(SonnenStatus status) {
		// GridFeedIn_W: positive = feed-in; negative = purchase from grid
		if (status.gridFeedInW() != null) {
			this._setActivePower(-status.gridFeedInW());
		}
		if (status.fac() != null) {
			this._setFrequency(Math.round(status.fac()));
		}
		if (status.uac() != null) {
			this._setVoltage(status.uac());
		}
	}

	@Override
	public void updateFromPowermeter(SonnenPowermeter powermeter) {
		if (powermeter == null) {
			return;
		}
		// The 'consumption' meter measures at the grid junction point; positive
		// values are purchases from the grid.
		this._setActivePowerL1(powermeter.wL1());
		this._setActivePowerL2(powermeter.wL2());
		this._setActivePowerL3(powermeter.wL3());
		this._setReactivePower(powermeter.varTotal());
		this._setCurrentL1(roundToInteger(powermeter.currentL1()));
		this._setCurrentL2(roundToInteger(powermeter.currentL2()));
		this._setCurrentL3(roundToInteger(powermeter.currentL3()));
		this._setVoltageL1(roundToInteger(powermeter.voltageL1N()));
		this._setVoltageL2(roundToInteger(powermeter.voltageL2N()));
		this._setVoltageL3(roundToInteger(powermeter.voltageL3N()));
		if (powermeter.kwhImported() != null) {
			this._setActiveConsumptionEnergy(powermeter.kwhImported() * 1000);
		}
		if (powermeter.kwhExported() != null) {
			this._setActiveProductionEnergy(powermeter.kwhExported() * 1000);
		}
	}

	private static Integer roundToInteger(Float value) {
		return value == null ? null : Math.round(value);
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.GRID;
	}

	@Override
	public String debugLog() {
		return "Meter:" + this.getActivePower().asString();
	}
}
