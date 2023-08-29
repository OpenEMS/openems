package io.openems.edge.controller.ess.fixactivepower;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.schedulable.Schedulable;
import io.openems.edge.energy.api.schedulable.Schedule.Handler;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.PowerConstraint;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.FixActivePower", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssFixActivePowerImpl extends AbstractOpenemsComponent
		implements ControllerEssFixActivePower, Controller, OpenemsComponent, TimedataProvider, Schedulable {

	private final CalculateActiveTime calculateCumulatedActiveTime = new CalculateActiveTime(this,
			ControllerEssFixActivePower.ChannelId.CUMULATED_ACTIVE_TIME);
	private final ScheduleHandler scheduleHandler = new ScheduleHandler();

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	public ControllerEssFixActivePowerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssFixActivePower.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (this.applyConfig(context, config)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		if (this.applyConfig(context, config)) {
			return;
		}
	}

	private boolean applyConfig(ComponentContext context, Config config) {
		this.scheduleHandler.applyStaticConfig(config);
		return OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		var config = this.scheduleHandler.getCurrentConfig();
		// TODO Update _Property-Channels

		var isActive = false;
		try {
			isActive = switch (config.mode()) {
			case MANUAL_ON -> {
				// Apply Active-Power Set-Point
				var acPower = getAcPower(this.ess, config.hybridEssMode(), config.power());
				PowerConstraint.apply(this.ess, this.id(), //
						config.phase(), Pwr.ACTIVE, config.relationship(), acPower);
				yield true; // is active
			}

			case MANUAL_OFF -> {
				// Do nothing
				yield false; // is not active
			}
			};

		} finally {
			this.calculateCumulatedActiveTime.update(isActive);
		}
	}

	/**
	 * Gets the required AC power set-point for AC- or Hybrid-ESS.
	 * 
	 * @param ess           the {@link ManagedSymmetricEss}; checked for
	 *                      {@link HybridEss}
	 * @param hybridEssMode the {@link HybridEssMode}
	 * @param power         the configured target power
	 * @return the AC power set-point
	 */
	protected static Integer getAcPower(ManagedSymmetricEss ess, HybridEssMode hybridEssMode, int power) {
		switch (hybridEssMode) {
		case TARGET_AC:
			return power;

		case TARGET_DC:
			if (ess instanceof HybridEss) {
				var pv = ess.getActivePower().orElse(0) - ((HybridEss) ess).getDcDischargePower().orElse(0);
				return pv + power; // Charge or Discharge
			} else {
				return power;
			}
		}

		return null; /* should never happen */
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public Handler<?, ?, ?> getScheduleHandler() {
		return this.scheduleHandler;
	}

}