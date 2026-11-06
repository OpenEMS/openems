package io.openems.edge.sonnen.ess;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.sonnen.core.OperatingMode;
import io.openems.edge.sonnen.core.SonnenCore;
import io.openems.edge.sonnen.core.SonnenStatus;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Sonnen.ESS", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
})
@GenerateTargetsFromReferences("core")
public class EssSonnenImpl extends AbstractOpenemsComponent
		implements EssSonnen, ManagedSymmetricEss, SymmetricEss, OpenemsComponent, EventHandler, TimedataProvider {

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, target = "(&(id=${config.core_id})(enabled=true))")
	private SonnenCore core;

	@Reference
	private Power power;

	@Reference
	private Sum sum;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	private Config config;

	private final CalculateEnergyFromPower calculateChargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY);
	private final CalculateEnergyFromPower calculateDischargeEnergy = new CalculateEnergyFromPower(this,
			SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY);

	public EssSonnenImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				EssSonnen.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (!config.enabled()) {
			return;
		}
		this._setCapacity(config.capacity());
		this._setMaxApparentPower(config.maxApparentPower());
		this.core.setEss(this);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		if (this.core != null) {
			this.core.setEss(null);
		}
		super.deactivate();
	}

	@Override
	public void updateFromStatus(SonnenStatus status) {
		// ESS channels
		this._setActivePower(status.pacTotalW());
		this._setSoc(status.usoc());
		final Boolean offGrid = status.isOffGrid();
		this.getGridModeChannel().setNextValue(//
				offGrid == null ? GridMode.UNDEFINED //
						: offGrid ? GridMode.OFF_GRID : GridMode.ON_GRID);
		this.channel(EssSonnen.ChannelId.OPERATING_MODE).setNextValue(status.operatingMode());

		// Limits
		var dischargeNotAllowed = Boolean.TRUE.equals(status.dischargeNotAllowed());
		this._setDischargeNotAllowed(dischargeNotAllowed);
		this.getAllowedChargePowerChannel().setNextValue(-this.config.maxChargePower());
		this._setAllowedDischargePower(dischargeNotAllowed ? 0 : this.config.maxDischargePower());
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		// The sonnenBatterie Write-API supports active power set-points only.
		this.channel(EssSonnen.ChannelId.SMART_MODE_NOT_WORKING_WITH_FILTER)
				.setNextValue(//
						this.config.controlMode() == ControlMode.SMART && this.power.isFilterEnabled());

		final var result = ApplyPowerHandler.calculate(//
				activePower, //
				this.config.controlMode(), //
				this.sum.getGridActivePower().get(), //
				this.getActivePower().get(), //
				this.config.defaultOperatingMode(), //
				this.config.maxChargePower(), //
				this.config.maxDischargePower());

		// The operating mode is always sent first, so that the set-points are
		// applied in 'Manual' mode.
		this.core.setOperatingMode(result.operatingMode());
		if (result.chargeSetpointW() != null) {
			this.core.setChargeSetpoint(result.chargeSetpointW());
		}
		if (result.dischargeSetpointW() != null) {
			this.core.setDischargeSetpoint(result.dischargeSetpointW());
		}
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> this.updateEnergyChannels();
		}
	}

	/**
	 * Calculates the ESS energy channels from the active power.
	 */
	private void updateEnergyChannels() {
		var activePower = this.getActivePower().get();
		if (activePower == null) {
			this.calculateChargeEnergy.update(null);
			this.calculateDischargeEnergy.update(null);
		} else if (activePower > 0) {
			// Discharge
			this.calculateChargeEnergy.update(0);
			this.calculateDischargeEnergy.update(activePower);
		} else {
			// Charge
			this.calculateChargeEnergy.update(activePower * -1);
			this.calculateDischargeEnergy.update(0);
		}
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public boolean isManaged() {
		return this.config.controlMode() != ControlMode.INTERNAL;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Mode:" + this.getOperatingModeChannel().value().asString();
	}
}
