package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.edge.energy.api.EnergyUtils.toEnergy;

import java.util.function.Supplier;

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
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.EnergyScheduleHandler;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.PowerConstraint;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
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
		implements ControllerEssFixActivePower, EnergySchedulable, Controller, OpenemsComponent, TimedataProvider {

	private final CalculateActiveTime calculateCumulatedActiveTime = new CalculateActiveTime(this,
			ControllerEssFixActivePower.ChannelId.CUMULATED_ACTIVE_TIME);
	private final EnergyScheduleHandler energyScheduleHandler;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	private Config config;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	public ControllerEssFixActivePowerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssFixActivePower.ChannelId.values() //
		);
		this.energyScheduleHandler = buildEnergyScheduleHandler(() -> new EshContext(this.config.mode(), //
				toEnergy(switch (this.config.phase()) {
				case ALL //
					-> this.config.power();
				case L1, L2, L3 //
					-> this.config.power() * 3;
				}), //
				this.config.relationship()));
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
		this.energyScheduleHandler.triggerReschedule();
	}

	private boolean applyConfig(ComponentContext context, Config config) {
		this.config = config;
		return OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		var isActive = false;
		try {
			isActive = switch (this.config.mode()) {
			case MANUAL_ON -> {
				// Apply Active-Power Set-Point
				var acPower = getAcPower(this.ess, this.config.hybridEssMode(), this.config.power());
				PowerConstraint.apply(this.ess, this.id(), //
						this.config.phase(), Pwr.ACTIVE, this.config.relationship(), acPower);
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

	/**
	 * Builds the {@link EnergyScheduleHandler}.
	 * 
	 * <p>
	 * This is public so that it can be used by the EnergyScheduler integration
	 * test.
	 * 
	 * @param context a supplier for the configured {@link EshContext}
	 * @return a {@link EnergyScheduleHandler}
	 */
	public static EnergyScheduleHandler buildEnergyScheduleHandler(Supplier<EshContext> context) {
		return EnergyScheduleHandler.of(//
				simContext -> context.get(), //
				(simContext, period, energyFlow, ctrlContext) -> {
					switch (ctrlContext.mode) {
					case MANUAL_ON:
						switch (ctrlContext.relationship) {
						case EQUALS -> energyFlow.setEss(ctrlContext.energy);
						case GREATER_OR_EQUALS -> energyFlow.setEssMaxCharge(-ctrlContext.energy);
						case LESS_OR_EQUALS -> energyFlow.setEssMaxDischarge(ctrlContext.energy);
						}
						break;
					case MANUAL_OFF:
						break;
					}
				});
	}

	public static record EshContext(Mode mode, int energy, Relationship relationship) {
	}

	@Override
	public EnergyScheduleHandler getEnergyScheduleHandler() {
		return this.energyScheduleHandler;
	}
}