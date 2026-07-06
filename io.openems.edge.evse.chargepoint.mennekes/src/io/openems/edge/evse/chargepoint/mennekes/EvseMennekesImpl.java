package io.openems.edge.evse.chargepoint.mennekes;

import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.evcs.api.Evcs.evaluatePhaseCountFromCurrent;
import static io.openems.edge.evse.api.common.ApplySetPoint.MIN_CURRENT;
import static io.openems.edge.evse.api.common.ApplySetPoint.convertMilliAmpereToWatt;
import static io.openems.edge.evse.chargepoint.mennekes.common.LogVerbosity.WRITES;
import static io.openems.edge.meter.api.ElectricityMeter.calculateAverageVoltageFromPhases;
import static io.openems.edge.meter.api.ElectricityMeter.calculateSumActivePowerFromPhases;
import static io.openems.edge.meter.api.ElectricityMeter.calculateSumCurrentFromPhases;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferenceCardinality.OPTIONAL;
import static org.osgi.service.component.annotations.ReferencePolicy.DYNAMIC;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.net.UnknownHostException;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.Phase.SingleOrThreePhase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch;
import io.openems.edge.evse.api.common.ApplyPhaseSwitch.PhaseSwitchDirection;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.bender.EvseChargePointBender;
import io.openems.edge.evse.chargepoint.mennekes.common.AbstractMennekes;
import io.openems.edge.evse.chargepoint.mennekes.common.LogVerbosity;
import io.openems.edge.evse.chargepoint.mennekes.common.Mennekes;
import io.openems.edge.evse.chargepoint.mennekes.enums.PhaseSwitchMode;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.meter.api.PhaseRotation;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evse.ChargePoint.Mennekes", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
@EventTopics({ //
		TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class EvseMennekesImpl extends AbstractMennekes implements EvseChargePoint, ElectricityMeter, Mennekes,
		OpenemsComponent, TimedataProvider, EventHandler, ModbusComponent {

	public static final int MAX_CURRENT = 16000; // [mA]

	private final Logger log = LoggerFactory.getLogger(EvseMennekesImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	private Config config;
	private SingleOrThreePhase lastUsedPhase = null;

	@Override
	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EvseMennekesImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				Mennekes.ChannelId.values(), //
				EvseChargePointBender.ChannelId.values(), //
				EvseChargePoint.ChannelId.values() //
		);

		calculateSumCurrentFromPhases(this);
		calculateSumActivePowerFromPhases(this);
		calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws UnknownHostException, OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId() /* Unit-ID */,
				this.cm, "Modbus", config.modbus_id())) {
			return;
		}

	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		if (this.config == null || this.isReadOnly()) {
			return ChargePointAbilities.create()//
					.build();
		}

		var phaseAbility = this.getPhaseAbility();

		final var minPower = convertMilliAmpereToWatt(phaseAbility, MIN_CURRENT);
		final var maxPower = convertMilliAmpereToWatt(phaseAbility, MAX_CURRENT);

		var setPointAbility = new ApplySetPoint.Ability.Watt(//
				phaseAbility, //
				minPower, //
				maxPower);

		var abilities = ChargePointAbilities.create() //
				.setApplySetPoint(setPointAbility) //
				.setIsEvConnected(this.isEvConnected()) //
				.setIsReadyForCharging(this.getIsReadyForCharging()); //

		if (this.isDynamicPhaseSwitchSupported()) {
			final var possibleSwitch = this.getPhaseSwitchAbility();
			if (possibleSwitch != null) {
				abilities.setPhaseSwitch(new ApplyPhaseSwitch(//
						possibleSwitch, //
						new ApplyPhaseSwitch.PhaseSwitchAbility.Internal()));
			}
		}

		return abilities.build();
	}

	private PhaseSwitchDirection getPhaseSwitchAbility() {
		var phaseCount = evaluatePhaseCountFromCurrent(//
				this.getCurrentL1().orElse(0), //
				this.getCurrentL2().orElse(0), //
				this.getCurrentL3().orElse(0) //
		);
		if (phaseCount == null) {
			return null;
		}
		var phase = this.getPhaseAbility();
		if (phase == SingleOrThreePhase.SINGLE_PHASE) {
			return PhaseSwitchDirection.TO_THREE_PHASE;
		} else {
			return PhaseSwitchDirection.TO_SINGLE_PHASE;
		}
	}

	private SingleOrThreePhase getPhaseAbility() {
		if (this.lastUsedPhase != null && this.isDynamicPhaseSwitchSupported()) {
			return this.lastUsedPhase;
		}
		return this.config.wiring();
	}

	private boolean isDynamicPhaseSwitchSupported() {
		return this.getPhaseSwitchMode().equals(PhaseSwitchMode.DYNAMIC_PHASE_SWITCH);
	}

	@Override
	public void apply(ChargePointActions actions) {
		final var ps = actions.phaseSwitch();
		if (ps != null) {
			this.lastUsedPhase = switch (ps.direction()) {
			case TO_SINGLE_PHASE -> SingleOrThreePhase.SINGLE_PHASE;
			case TO_THREE_PHASE -> SingleOrThreePhase.THREE_PHASE;
			};
		}

		// Set ApplySetPoint
		final var power = actions.getApplySetPointInWatt().value();

		try {
			this.log(WRITES, "Setting Power to " + power);
			this.getApplyPowerLimitChannel().setNextWriteValue(power);
		} catch (OpenemsNamedException e) {
			this.logWarn(WRITES, "Failed to apply original limit " + power + "W." + e);
		}
	}

	private void log(LogVerbosity logVerbosity, String text) {
		if (this.config.logVerbosity() == logVerbosity) {
			this.logInfo(this.log, text);
		}
	}

	private void logWarn(LogVerbosity logVerbosity, String text) {
		if (this.config.logVerbosity() == logVerbosity) {
			this.logWarn(this.log, text);
		}
	}

	@Override
	public void handleEvent(Event event) {
		this.benderHandleEvent(event);
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
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

}
