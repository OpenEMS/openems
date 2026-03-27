package io.openems.edge.evse.chargepoint.mennekes;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static io.openems.edge.common.event.EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE;
import static io.openems.edge.evcs.api.Evcs.evaluatePhaseCountFromCurrent;
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
import io.openems.edge.common.type.Phase;
import io.openems.edge.evse.api.chargepoint.EvseChargePoint;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointAbilities;
import io.openems.edge.evse.api.chargepoint.Profile.ChargePointActions;
import io.openems.edge.evse.api.common.ApplySetPoint;
import io.openems.edge.evse.chargepoint.bender.EvseChargePointBender;
import io.openems.edge.evse.chargepoint.mennekes.common.AbstractMennekes;
import io.openems.edge.evse.chargepoint.mennekes.common.Mennekes;
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

	public static final int DEFAULT_MAX_CURRENT = 16;
	public static final int DEFAULT_MIN_CURRENT = 6;

	private final Logger log = LoggerFactory.getLogger(EvseMennekesImpl.class);

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = DYNAMIC, policyOption = GREEDY, cardinality = OPTIONAL)
	private volatile Timedata timedata = null;

	private Config config;

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

		this.handleSoftwareVersion();
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
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case TOPIC_CYCLE_BEFORE_PROCESS_IMAGE -> {
			var state = this.isReadyForCharging();
			setValue(this, EvseChargePoint.ChannelId.IS_READY_FOR_CHARGING, state);
		}
		}
	}

	@Override
	public ChargePointAbilities getChargePointAbilities() {
		if (this.config == null) {
			return ChargePointAbilities.create()//
					.build();
		}

		final var isEvConnected = this.isEvConnected();
		final var maxCurrent = this.getMaxCurrent();
		final var minCurrent = this.getMinCurrent();

		final var phaseCount = evaluatePhaseCountFromCurrent(//
				this.getCurrentL1().orElse(0), //
				this.getCurrentL2().orElse(0), //
				this.getCurrentL3().orElse(0));
		final Phase.SingleOrThreePhase phase;
		if (phaseCount != null && phaseCount == 1) {
			phase = Phase.SingleOrThreePhase.SINGLE_PHASE;
		} else {
			phase = Phase.SingleOrThreePhase.THREE_PHASE;
		}
		if (this.isReadOnly()) {
			return ChargePointAbilities.create()//
					.build();
		}
		return ChargePointAbilities.create() //
				.setApplySetPoint(new ApplySetPoint.Ability.Ampere(phase, //
						minCurrent == null ? DEFAULT_MIN_CURRENT : minCurrent, //
						maxCurrent == null ? DEFAULT_MAX_CURRENT : maxCurrent)) //
				.setIsEvConnected(isEvConnected) //
				.setIsReadyForCharging(this.getIsReadyForCharging()) //
				.build();
	}

	@Override
	public void apply(ChargePointActions actions) {
		final var current = actions.getApplySetPointInAmpere().value();
		switch (this.config.logVerbosity()) {
		case WRITES -> this.logInfo(this.log, "Setting Current to " + current);
		case DEBUG_LOG, NONE, READS -> doNothing();
		}
		try {
			this.getApplyCurrentLimitChannel().setNextWriteValue(current);
		} catch (OpenemsNamedException e) {
			this.logWarn(this.log, "Failed to apply current limit. " + e);
		}

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
