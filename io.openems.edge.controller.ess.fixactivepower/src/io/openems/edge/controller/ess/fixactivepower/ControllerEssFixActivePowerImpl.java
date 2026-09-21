package io.openems.edge.controller.ess.fixactivepower;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.controller.ess.fixactivepower.EnergyScheduler.buildEnergyScheduleHandler;
import static io.openems.edge.controller.ess.fixactivepower.SystemLimitHelper.clampToSystemLimits;
import static io.openems.edge.controller.ess.fixactivepower.SystemLimitHelper.SystemLimits.fromMeta;
import static io.openems.edge.energy.api.handler.RescheduleMode.OPTIMIZE_CURRENT_PERIOD;
import static io.openems.edge.ess.power.api.Relationship.EQUALS;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.Phase;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.fixactivepower.SystemLimitHelper.SystemLimits;
import io.openems.edge.controller.ess.fixactivepower.enums.HybridEssMode;
import io.openems.edge.controller.ess.fixactivepower.enums.Mode;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
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
@GenerateTargetsFromReferences("ess")
public class ControllerEssFixActivePowerImpl extends AbstractOpenemsComponent
		implements ControllerEssFixActivePower, EnergySchedulable, Controller, OpenemsComponent, TimedataProvider {

	private static final Pwr DEFAULT_PWR = Pwr.ACTIVE;
	private static final Relationship DEFAULT_RELATIONSHIP = EQUALS;
	private static final Phase.SingleOrAllPhase DEFAULT_PHASE = Phase.SingleOrAllPhase.ALL;

	private final Logger log = LoggerFactory.getLogger(ControllerEssFixActivePowerImpl.class);

	private final AtomicBoolean isActive = new AtomicBoolean(false);
	private final CalculateActiveTime calculateCumulatedActiveTime = new CalculateActiveTime(this,
			ControllerEssFixActivePower.ChannelId.CUMULATED_ACTIVE_TIME);
	private final FallbackHandler fallbackHandler = new FallbackHandler();

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.ess_id})(enabled=true))")
	private ManagedSymmetricEss ess;

	@Reference
	private Meta meta;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private Sum sum;

	private Config config;
	private EnergyScheduleHandler energyScheduleHandler;

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
		this.fallbackHandler.clear();
		this.applyConfig(config);
		this.energyScheduleHandler = buildEnergyScheduleHandler(this, this::buildEnergySchedulerConfig);
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.fallbackHandler.clear();
		this.applyConfig(config);

		this.energyScheduleHandler.triggerReschedule("ControllerEssFixActivePowerImpl::modified()",
				OPTIMIZE_CURRENT_PERIOD);

		this.resetIgnoreSystemLimitsPermissionsIfRequired(config);
	}

	private EnergyScheduler.Config buildEnergySchedulerConfig() {
		if (!this.config.enabled()) {
			return null;
		}

		return switch (this.config.mode()) {
		case MANUAL_OFF -> null;
		case MANUAL_ON -> new EnergyScheduler.Config(//
				Mode.MANUAL_ON, //
				switch (this.config.phase()) {
				case ALL -> this.config.power();
				case L1, L2, L3 -> this.config.power() * 3;
				}, //
				null);
		case CHARGE_ONCE -> this.meta.getIsEssChargeFromGridAllowed() //
				? new EnergyScheduler.Config(//
						Mode.CHARGE_ONCE, //
						this.config.chargeOncePower(), //
						this.config.chargeOnceTargetSocEnable() ? this.config.chargeOnceTargetSoc() : null) //
				: null;
		case DISCHARGE_ONCE -> this.meta.getIsEssDischargeToGridAllowed() //
				? new EnergyScheduler.Config(//
						Mode.DISCHARGE_ONCE, //
						this.config.dischargeOncePower(), //
						this.config.dischargeOnceTargetSocEnable() ? this.config.dischargeOnceTargetSoc() : null) //
				: null;
		};
	}

	private void applyConfig(Config config) {
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		this.isActive.set(false);

		var controllerTarget = switch (this.config.mode()) {
		case CHARGE_ONCE, DISCHARGE_ONCE -> this.calculateEssTargetForModeOnce();
		case MANUAL_ON -> this.calculateEssTargetForManualMode();
		case MANUAL_OFF -> ControllerTarget.inactiveNoWarning();
		};

		var systemLimits = fromMeta(this.meta);
		this.applyControllerTargetConsideringSystemLimits(controllerTarget, systemLimits);
		this.calculateCumulatedActiveTime.update(this.isActive.get());
	}

	private ControllerTarget calculateEssTargetForManualMode() {
		var requestedTarget = new PowerTarget(this.config.phase(), Pwr.ACTIVE, this.config.relationship(),
				this.config.hybridEssMode(), this.config.power());
		return ControllerTarget.activeWithTarget(requestedTarget);
	}

	private ControllerTarget calculateEssTargetForModeOnce() throws OpenemsNamedException {
		if (this.fallbackHandler.isFallbackTimeoutReached(this.ess, this.config.mode(),
				this.componentManager.getClock())) {
			this.log.warn("Fallback triggered for mode [{}]: allowed power stayed at 0 W for {} minutes.",
					this.config.mode(), FallbackHandler.FALLBACK_TIMEOUT_MINUTES);
			return ControllerTarget.inactiveWithReset();
		}

		var requestedTarget = targetForModeOnce(this.config, this.ess.getSoc().getOrError());
		return requestedTarget.map(ControllerTarget::activeWithTarget).orElseGet(ControllerTarget::inactiveWithReset);
	}

	static Optional<PowerTarget> targetForModeOnce(Config config, int soc) {
		return switch (config.mode()) {
		case CHARGE_ONCE -> {
			// Stop if target SoC or configured target is reached.
			if (soc >= 100 || (config.chargeOnceTargetSocEnable() && soc >= config.chargeOnceTargetSoc())) {
				yield Optional.empty();
			}
			yield Optional.of(PowerTarget.fromDcPowerWithDefaults(-Math.abs(config.chargeOncePower())));
		}

		case DISCHARGE_ONCE -> {
			// Stop if target SoC or configured target is reached.
			if (soc <= 0 || (config.dischargeOnceTargetSocEnable() && soc <= config.dischargeOnceTargetSoc())) {
				yield Optional.empty();
			}
			yield Optional.of(PowerTarget.fromDcPowerWithDefaults(config.dischargeOncePower()));
		}
		default -> Optional.empty();
		};
	}

	private void applyControllerTargetConsideringSystemLimits(ControllerTarget controllerTarget,
			SystemLimits systemLimits) {
		this.setInformationChannels(null, false, false, false);
		if (controllerTarget.resetModeToManualOff()) {
			this.resetModeToManualOff();
			this.fallbackHandler.clear();
			return;
		}

		var request = controllerTarget.requestedPowerTarget();
		if (request == null) {
			return;
		}

		var limitedRequest = clampToSystemLimits(this.id(), request, this.ess, systemLimits,
				this.sum.getGridActivePower().get(), this.config.ignoreSystemLimitsPermissionsOnce(),
				this.config.considerSystemLimits());

		final var powerTarget = limitedRequest.powerTarget();
		if (powerTarget != null) {
			try {
				this.applyPowerConstraint(powerTarget, limitedRequest.limitedByMetaLimit());
			} catch (OpenemsNamedException e) {
				this.logWarn(this.log,
						"Failed to apply power constraint for target " + powerTarget.power() + " W:" + e.getMessage());
			}
		}
		this.setInformationChannels(powerTarget, limitedRequest.limitedByEssHardware(),
				limitedRequest.limitedByMetaLimit());
	}

	private void resetModeToManualOff() {
		final var property = "mode";
		final var requiredValue = Mode.MANUAL_OFF.name();

		try {
			var pid = this.servicePid();
			if (pid.isEmpty()) {
				return;
			}

			Configuration configuration = this.cm.getConfiguration(pid, "?");
			var properties = configuration.getProperties();
			if (properties == null) {
				return;
			}

			var existingValue = properties.get(property);
			if (requiredValue.equals(existingValue)) {
				return;
			}

			properties.put(property, requiredValue);
			configuration.update(properties);
		} catch (IOException | SecurityException e) {
			// Keep cycle deterministic; component will retry next cycle if needed.
			this.logWarn(this.log, "Failed to reset the property mode to " + requiredValue + ": " + e.getMessage());
		}
	}

	void resetIgnoreSystemLimitsPermissionsIfRequired(Config config) {
		if (!config.ignoreSystemLimitsPermissionsOnce()) {
			return;
		}

		if (this.config.mode() == Mode.CHARGE_ONCE || this.config.mode() == Mode.DISCHARGE_ONCE) {
			// Keep true until the event finished.
			return;
		}

		// Reset the ignoreSystemLimitsPermissionsOnce to false.
		final var property = "ignoreSystemLimitsPermissionsOnce";

		try {
			var pid = this.servicePid();
			if (pid.isEmpty()) {
				return;
			}

			Configuration configuration = this.cm.getConfiguration(pid, "?");
			var properties = configuration.getProperties();
			if (properties == null) {
				return;
			}

			properties.put(property, false);
			configuration.update(properties);
		} catch (IOException | SecurityException e) {
			// Keep cycle deterministic; component will retry next cycle if needed.
			this.logWarn(this.log,
					"Failed to reset the property ignoreSystemLimitsPermissionsOnce to false: " + e.getMessage());
		}
	}

	private void applyPowerConstraint(PowerTarget powerTarget, boolean powerWasLimitedByMeta)
			throws OpenemsNamedException {
		var acPower = getAcPower(this.ess, powerTarget.hybridEssMode, powerTarget.power);
		this.isActive.set(true);
		if (hasPowerTargetDefaultPowerProperties(powerTarget) && powerWasLimitedByMeta) {
			/*
			 * Only here a Filter is applied. This is especially needed when the target was
			 * limited by the gridLimit, due to volatile gridActivePower.
			 */
			this.ess.setActivePowerEqualsWithFilter(acPower);
			return;
		}
		PowerConstraint.apply(this.ess, this.id(), powerTarget.phase(), powerTarget.pwr(), powerTarget.relationship(),
				acPower);
	}

	private static boolean hasPowerTargetDefaultPowerProperties(PowerTarget powerTarget) {
		return powerTarget.phase() == Phase.SingleOrAllPhase.ALL && powerTarget.pwr() == Pwr.ACTIVE
				&& powerTarget.relationship() == EQUALS;
	}

	private void setInformationChannels(PowerTarget powerTarget, boolean limitedByEssHardware,
			boolean limitedByMetaLimit) {
		if (powerTarget == null) {
			this.setInformationChannels(null, limitedByEssHardware, limitedByMetaLimit, true);
			return;
		}
		this.setInformationChannels(powerTarget.power(), limitedByEssHardware, limitedByMetaLimit, false);
	}

	private void setInformationChannels(Integer realTarget, boolean limitedByEssHardware, boolean limitedByMetaLimit,
			boolean hasNoLimitApplied) {
		setValue(this, ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_ESS_HARDWARE, limitedByEssHardware);
		setValue(this, ControllerEssFixActivePower.ChannelId.SETPOINT_LIMITED_BY_META_LIMIT, limitedByMetaLimit);
		setValue(this, ControllerEssFixActivePower.ChannelId.NO_LIMIT_APPLIED, hasNoLimitApplied);
		setValue(this, ControllerEssFixActivePower.ChannelId.TARGET_AFTER_LIMITATIONS, realTarget);
	}

	record ControllerTarget(PowerTarget requestedPowerTarget, boolean resetModeToManualOff) {

		private static ControllerTarget activeWithTarget(PowerTarget powerTarget) {
			return new ControllerTarget(powerTarget, false);
		}

		private static ControllerTarget inactiveWithReset() {
			return new ControllerTarget(null, true);
		}

		private static ControllerTarget inactiveNoWarning() {
			return new ControllerTarget(null, false);
		}
	}

	public record PowerTarget(Phase.SingleOrAllPhase phase, Pwr pwr, Relationship relationship,
			HybridEssMode hybridEssMode, int power) {

		static PowerTarget fromDcPowerWithDefaults(int dcPower) {
			return new PowerTarget(DEFAULT_PHASE, DEFAULT_PWR, DEFAULT_RELATIONSHIP, HybridEssMode.TARGET_DC, dcPower);
		}

		static PowerTarget withAcPower(PowerTarget powerTarget, int acPower) {
			return new PowerTarget(powerTarget.phase, powerTarget.pwr, powerTarget.relationship,
					HybridEssMode.TARGET_AC, acPower);
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
	protected static int getAcPower(ManagedSymmetricEss ess, HybridEssMode hybridEssMode, int power) {
		return switch (hybridEssMode) {
		case TARGET_AC -> power;

		case TARGET_DC -> //
			switch (ess) {
			case HybridEss he -> {
				var pv = ess.getActivePower().orElse(0) - he.getDcDischargePower().orElse(0);
				yield pv + power; // Charge or Discharge
			}
			default -> power;
			};
		};
	}

	/**
	 * Gets the required DC power set-point for AC- or Hybrid-ESS.
	 *
	 * @param ess           the {@link ManagedSymmetricEss}; checked for
	 *                      {@link HybridEss}
	 * @param hybridEssMode the {@link HybridEssMode}
	 * @param power         the configured target power
	 * @return the DC power set-point
	 */
	protected static int getDcPower(ManagedSymmetricEss ess, HybridEssMode hybridEssMode, int power) {
		return switch (hybridEssMode) {
		case TARGET_DC -> power;

		case TARGET_AC -> //
			switch (ess) {
			case HybridEss he -> {
				var pv = ess.getActivePower().orElse(0) - he.getDcDischargePower().orElse(0);
				yield power - pv; // Charge or Discharge
			}
			default -> power;
			};
		};
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public EnergyScheduleHandler getEnergyScheduleHandler() {
		return this.energyScheduleHandler;
	}
}
