package io.openems.edge.sonnen.pv;

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
		name = "Sonnen.PV", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("core")
public class PvSonnenImpl extends AbstractOpenemsComponent
		implements PvSonnen, ElectricityMeter, OpenemsComponent {

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, target = "(&(id=${config.core_id})(enabled=true))")
	private SonnenCore core;

	public PvSonnenImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				PvSonnen.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (!config.enabled()) {
			return;
		}
		this.core.setPv(this);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.core != null) {
			this.core.setPv(null);
		}
		super.deactivate();
	}

	@Override
	public void updateFromStatus(SonnenStatus status) {
		// Production_W: positive = PV production; the ElectricityMeter
		// convention is negative = production.
		if (status.productionW() != null) {
			this._setActivePower(-status.productionW());
		}
	}

	@Override
	public void updateFromPowermeter(SonnenPowermeter powermeter) {
		if (powermeter == null) {
			return;
		}
		this._setActivePowerL1(negate(powermeter.wL1()));
		this._setActivePowerL2(negate(powermeter.wL2()));
		this._setActivePowerL3(negate(powermeter.wL3()));
		if (powermeter.kwhExported() != null) {
			this._setActiveProductionEnergy(powermeter.kwhExported() * 1000);
		}
	}

	private static Integer negate(Integer value) {
		return value == null ? null : -value;
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}

	@Override
	public String debugLog() {
		return "PV:" + this.getActivePower().asString();
	}
}
