package io.openems.edge.simulator.evse.chargepoint;

import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.meter.api.PhaseRotation.setPhaseRotatedActivePowerChannels;
import static io.openems.edge.meter.api.PhaseRotation.setPhaseRotatedCurrentChannels;
import static io.openems.edge.meter.api.PhaseRotation.setPhaseRotatedVoltageChannels;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.type.Phase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.simulator.evse.chargepoint.enums.PhaseSwitchState;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.Evse.ChargePoint", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class SimulatorEvseChargepointImpl extends AbstractOpenemsComponent implements SimulatorEvseChargepoint,
		OpenemsComponent, TimedataProvider, EvseChargePoint, EventHandler, ElectricityMeter {

	private final CalculateEnergyFromPower calculateEnergyL1 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1);
	private final CalculateEnergyFromPower calculateEnergyL2 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2);
	private final CalculateEnergyFromPower calculateEnergyL3 = new CalculateEnergyFromPower(this,
			ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3);

	private Config config;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata;

	@Reference
	protected ComponentManager componentManager;

	private Phase.SingleOrThreePhase currentPhases;

	public SimulatorEvseChargepointImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				EvseChargePoint.ChannelId.values(), //
				SimulatorEvseChargepoint.ChannelId.values() //
		);
		ElectricityMeter.calculateSumCurrentFromPhases(this);
		ElectricityMeter.calculateSumActivePowerFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	private void applyConfig(Config config) {
		this.config = config;
		setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, config.vehicleConnected());

		this.setActivePhase(switch (this.config.wiring()) {
		case Phase.SingleOrThreePhase.SINGLE_PHASE -> PhaseSwitchState.SINGLE;
		case Phase.SingleOrThreePhase.THREE_PHASE -> PhaseSwitchState.THREE;
		});

		this.applyPowerPresets();
	}

	private void setActivePhase(PhaseSwitchState phaseSwitchState) {
		setValue(this, SimulatorEvseChargepoint.ChannelId.PHASE_SWITCH_STATE, phaseSwitchState);
		this.currentPhases = phaseSwitchState.actual;
	}

	private void applyPowerPresets() {
		if (this.config.readOnly() && this.config.vehicleConnected()) {
			this.applyPower(this.config.minCurrent() * this.config.voltage() / 1000);
			this.applyCurrent(this.config.minCurrent());
			this.applyVoltage(this.config.voltage());
		} else {
			this.applyPower(0);
			this.applyCurrent(0);
			this.applyVoltage(0);
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		return new StringBuilder() //
				.append("L:").append(this.getActivePower().asString()) //
				.append("|P:").append(this.getPhaseSwitchState().getValue()) //
				.toString();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}

		if (event.getTopic().equals(EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE)) {
			this.calculateEnergyL1.update(this.getActivePowerL1Channel().getNextValue().get());
			this.calculateEnergyL2.update(this.getActivePowerL2Channel().getNextValue().get());
			this.calculateEnergyL3.update(this.getActivePowerL3Channel().getNextValue().get());
		}
	}

	@Override
	public void apply(ChargePointActions actions) {
		if (!this.isEnabled() || this.isReadOnly()) {
			return;
		}

		if (actions.phaseSwitch() != null) {
			this.applyPhaseSwitch(actions.phaseSwitch());
		}

		var powerInWatt = actions.getApplySetPointInMilliAmpere().value() * this.config.voltage() / 1000;
		this.applyPower(powerInWatt);
		this.applyCurrent(actions.getApplySetPointInMilliAmpere().value());
		this.applyVoltage(this.config.voltage());
	}

	private void applyPhaseSwitch(ApplyPhaseSwitch phaseSwitch) {
		switch (phaseSwitch.direction()) {
		case ApplyPhaseSwitch.PhaseSwitchDirection.TO_SINGLE_PHASE -> this.setActivePhase(PhaseSwitchState.SINGLE);
		case ApplyPhaseSwitch.PhaseSwitchDirection.TO_THREE_PHASE -> this.setActivePhase(PhaseSwitchState.THREE);
		}
	}

	private void applyPower(int powerInWattForSinglePhase) {
		var threePhasePower = this.isThreePhase() ? powerInWattForSinglePhase : 0;
		setPhaseRotatedActivePowerChannels(this, powerInWattForSinglePhase, threePhasePower, threePhasePower);
	}

	private void applyCurrent(int currentInMilliAmpsForSinglePhase) {
		int threePhaseCurrent = this.isThreePhase() ? currentInMilliAmpsForSinglePhase : 0;
		setPhaseRotatedCurrentChannels(this, currentInMilliAmpsForSinglePhase, threePhaseCurrent, threePhaseCurrent);
	}

	private void applyVoltage(int voltage) {
		int voltageInMilliVolts = voltage * 1000;
		int threePhaseVoltage = this.isThreePhase() ? voltageInMilliVolts : 0;
		setPhaseRotatedVoltageChannels(this, voltageInMilliVolts, threePhaseVoltage, threePhaseVoltage);
		setValue(this, ElectricityMeter.ChannelId.VOLTAGE, voltageInMilliVolts);
	}

	@Override
	public PhaseRotation getPhaseRotation() {
		return this.config.phaseRotation();
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public boolean isReadOnly() {
		return this.config.readOnly();
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		if (this.config.readOnly()) {
			return null;
		}

		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.MilliAmpere(this.getPhase(), this.config.minCurrent(),
						this.config.maxCurrent())) //
				.setIsReadyForCharging(this.config.vehicleConnected()) //
				.setIsEvConnected(this.config.vehicleConnected()) //
				.setPhaseSwitchManual(this.getPhaseSwitchAbility(), this.getOppositePhaseApplySetPointAbility()) //
				.build();
	}

	private ApplySetPoint.Ability.Watt getOppositePhaseApplySetPointAbility() {
		if (!this.supportsPhaseSwitching()) {
			return null;
		}

		final var oppositePhase = switch (this.getPhase()) {
		case SINGLE_PHASE -> Phase.SingleOrThreePhase.THREE_PHASE;
		case THREE_PHASE -> Phase.SingleOrThreePhase.SINGLE_PHASE;
		};

		return new ApplySetPoint.Ability.Watt(oppositePhase,
				ApplySetPoint.convertMilliAmpereToWatt(oppositePhase, this.config.minCurrent()),
				ApplySetPoint.convertMilliAmpereToWatt(oppositePhase, this.config.maxCurrent()));
	}

	protected boolean supportsPhaseSwitching() {
		return this.config.supportsPhaseSwitching() //
				&& this.config.wiring() == Phase.SingleOrThreePhase.THREE_PHASE;
	}

	private ApplyPhaseSwitch.PhaseSwitchDirection getPhaseSwitchAbility() {
		if (!this.supportsPhaseSwitching()) {
			return null;
		}

		return switch (this.getPhaseSwitchState()) {
		case SINGLE -> ApplyPhaseSwitch.PhaseSwitchDirection.TO_THREE_PHASE;
		case UNDEFINED, THREE -> ApplyPhaseSwitch.PhaseSwitchDirection.TO_SINGLE_PHASE;
		};
	}

	private boolean isThreePhase() {
		return switch (this.currentPhases) {
		case THREE_PHASE -> true;
		case SINGLE_PHASE -> false;
		};
	}

	private Phase.SingleOrThreePhase getPhase() {
		return this.currentPhases;
	}
}
