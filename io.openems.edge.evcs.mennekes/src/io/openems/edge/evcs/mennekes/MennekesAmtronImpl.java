package io.openems.edge.evcs.mennekes;

import java.util.function.Consumer;

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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Mennekes", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
})
public class MennekesAmtronImpl extends AbstractOpenemsModbusComponent
		implements Evcs, ManagedEvcs, OpenemsComponent, ModbusComponent, EventHandler, MennekesAmtron {

	private final Logger log = LoggerFactory.getLogger(MennekesAmtronImpl.class);

	// TODO: Add functionality to distinguish between firmware version. For firmware
	// version >= 5.22 there are several new registers. Currently it is programmed
	// for firmware version 5.14.
	private boolean softwareVersionSmallerThan_5_22 = true;

	protected Config config;

	@Reference
	private EvcsPower evcsPower;

	@Reference
	protected ConfigurationAdmin cm;

	/**
	 * Handles charge states.
	 */
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	/**
	 * Processes the controller's writes to this evcs component.
	 */
	private final WriteHandler writeHandler = new WriteHandler(this);

	public MennekesAmtronImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				MennekesAmtron.ChannelId.values());
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		/*
		 * Calculates the maximum and minimum hardware power dynamically by listening on
		 * the fixed hardware limit and the phases used for charging
		 */
		Evcs.addCalculatePowerLimitListeners(this);

		this.applyConfig(context, config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfig(context, config);
	}

	private void applyConfig(ComponentContext context, Config config) {
		this.config = config;
		this._setChargingType(ChargingType.AC);
		this._setFixedMinimumHardwarePower(this.getConfiguredMinimumHardwarePower());
		this._setFixedMaximumHardwarePower(this.getConfiguredMaximumHardwarePower());
		this._setPowerPrecision(230);
		this._setPhases(3);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.writeHandler.run();
			break;
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		// TODO: Distinguish between firmware version. For firmware version >= 5.22
		// there are several new registers.
		ModbusProtocol modbusProtocol = new ModbusProtocol(this,
				new FC3ReadRegistersTask(100, Priority.HIGH,
						m(MennekesAmtron.ChannelId.RAW_FIRMWARE_VERSION,
								new UnsignedDoublewordElement(100).wordOrder(WordOrder.MSWLSW))),
				new FC3ReadRegistersTask(104, Priority.HIGH,
						m(MennekesAmtron.ChannelId.OCPP_CP_STATUS, new UnsignedWordElement(104)),
						m(MennekesAmtron.ChannelId.ERROR_CODES_1, new UnsignedDoublewordElement(105)),
						m(MennekesAmtron.ChannelId.ERROR_CODES_2, new UnsignedDoublewordElement(107)),
						m(MennekesAmtron.ChannelId.ERROR_CODES_3, new UnsignedDoublewordElement(109)),
						m(MennekesAmtron.ChannelId.ERROR_CODES_4, new UnsignedDoublewordElement(111))),
				new FC3ReadRegistersTask(122, Priority.HIGH,
						m(MennekesAmtron.ChannelId.VEHICLE_STATE, new UnsignedWordElement(122))),
				new FC3ReadRegistersTask(131, Priority.HIGH,
						m(MennekesAmtron.ChannelId.SAFE_CURRENT, new UnsignedWordElement(131))),
				new FC3ReadRegistersTask(200, Priority.HIGH,
						m(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(200))),
				new FC3ReadRegistersTask(206, Priority.HIGH,
						m(MennekesAmtron.ChannelId.ACTIVE_POWER_L1, new UnsignedDoublewordElement(206)),
						m(MennekesAmtron.ChannelId.ACTIVE_POWER_L2, new UnsignedDoublewordElement(208)),
						m(MennekesAmtron.ChannelId.ACTIVE_POWER_L3, new UnsignedDoublewordElement(210)),
						m(MennekesAmtron.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(212)),
						m(MennekesAmtron.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(214)),
						m(MennekesAmtron.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(216))),

				// TODO: Check Nature Channels - if some missing, eg. session energy
				new FC3ReadRegistersTask(705, Priority.HIGH,
						m(Evcs.ChannelId.ENERGY_SESSION, new UnsignedWordElement(705)),
						m(MennekesAmtron.ChannelId.MAX_CURRENT_EV, new UnsignedWordElement(706)),
						m(MennekesAmtron.ChannelId.RAW_CHARGING_SESSION_START_TIME, new UnsignedDoublewordElement(707)),
						m(MennekesAmtron.ChannelId.CHARGE_DURATION, new UnsignedWordElement(709)),
						m(MennekesAmtron.ChannelId.RAW_CHARGING_STOP_TIME, new UnsignedDoublewordElement(710)),
						m(MennekesAmtron.ChannelId.MIN_CURRENT_LIMIT, new UnsignedWordElement(712))),

				new FC3ReadRegistersTask(1000, Priority.HIGH,
						m(MennekesAmtron.ChannelId.EMS_CURRENT_LIMIT, new UnsignedWordElement(1000))),
				new FC16WriteRegistersTask(1000,
						m(MennekesAmtron.ChannelId.APPLY_CURRENT_LIMIT, new UnsignedWordElement(1000))));

		// Calculates required Channels from other existing Channels.
		this.addCalculateChannelListeners();

		this.addStatusListener();

		return modbusProtocol;
	}

	private void addCalculateChannelListeners() {

		// TODO: Process Error Codes
		final Consumer<Value<Integer>> processErrorCodes = ignore -> {
			String error1 = Integer.toHexString(this.getErrorCode1Channel().getNextValue().orElse(0));
			String error2 = Integer.toHexString(this.getErrorCode2Channel().getNextValue().orElse(0));
			String error3 = Integer.toHexString(this.getErrorCode3Channel().getNextValue().orElse(0));
			String error4 = Integer.toHexString(this.getErrorCode4Channel().getNextValue().orElse(0));
			String errorcode = error4 + error3 + error2 + error1;
			int result = Integer.parseInt(errorcode);
			this._setChargingstationCommunicationFailed(result > 0);
			if (result > 0) {
				this.log.error("An Error has accured. Error code: " + errorcode
						+ " Error Code processing not yet implemented.");
			}
		};
		this.getErrorCode1Channel().onSetNextValue(processErrorCodes);
		this.getErrorCode2Channel().onSetNextValue(processErrorCodes);
		this.getErrorCode3Channel().onSetNextValue(processErrorCodes);
		this.getErrorCode4Channel().onSetNextValue(processErrorCodes);

		// TODO: Power is given by the charger since firmware 5.22
		final Consumer<Value<Integer>> powerChannels = ignore -> {

			this._setChargePower(TypeUtils.sum(this.getActivePowerL1().orElse(0), this.getActivePowerL2().orElse(0),
					this.getActivePowerL3().orElse(0)));

			int phases = 3;
			int powerThreshold = 50; // in W
			if (this.getActivePowerL1().orElse(0) > powerThreshold) {
				phases = phases + 1;
			}
			if (this.getActivePowerL2().orElse(0) > powerThreshold) {
				phases = phases + 1;
			}
			if (this.getActivePowerL3().orElse(0) > powerThreshold) {
				phases = phases + 1;
			}
			this._setPhases(phases);
		};

		this.getActivePowerL1Channel().onSetNextValue(powerChannels);
		this.getActivePowerL2Channel().onSetNextValue(powerChannels);
		this.getActivePowerL3Channel().onSetNextValue(powerChannels);

	}

	private void addStatusListener() {
		this.channel(MennekesAmtron.ChannelId.OCPP_CP_STATUS).onSetNextValue(s -> {
			var currentStatus = Status.UNDEFINED;
			OcppStateMennekes rawState = s.asEnum();
			/**
			 * Maps the raw state into a {@link Status}.
			 */
			switch (rawState) {

			case AVAILABLE:
				this._setStatus(Status.NOT_READY_FOR_CHARGING);
				break;
			case CHARGING:
				currentStatus = Status.CHARGING;
				break;
			case FAULTED:
				currentStatus = Status.ERROR;
				break;
			case FINISHING:
				currentStatus = Status.CHARGING_FINISHED;
				break;
			case PREPARING:
				currentStatus = Status.READY_FOR_CHARGING;
				break;
			case RESERVED:
				currentStatus = Status.NOT_READY_FOR_CHARGING;
				break;
			case SUSPENDEDEV:
			case SUSPENDEDEVSE:
				currentStatus = Status.CHARGING_REJECTED;
				break;
			case OCCUPIED:
				if (this.isCharging()) {
					currentStatus = Status.CHARGING;
				} else {
					currentStatus = Status.ENERGY_LIMIT_REACHED;
				}
				break;
			case UNAVAILABLE:
				currentStatus = Status.ERROR;
				break;
			case UNDEFINED:
				break;
			}

			this._setStatus(currentStatus);
		});
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return Math.round(this.config.minHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return Math.round(this.config.maxHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue();
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		return 30;
	}

	@Override
	public ChargeStateHandler getChargeStateHandler() {
		return this.chargeStateHandler;
	}

	@Override
	public void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public String debugLog() {
		return "Limit:" + this.getSetChargePowerLimit().orElse(null) + "|" + this.getStatus().getName();
	}

	public boolean isCharging() {
		return this.getChargePower().orElse(0) > 0;
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {
		var phases = this.getPhasesAsInt();
		var current = Math.round(power / phases / 230f);

		/*
		 * Limits the charging value because Mennekes knows only values between 6 and 32
		 * A
		 */
		current = Math.min(current, 32);

		if (current < 6) {
			current = 0;
		}

		this.setApplyCurrentLimit(current);
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws Exception {
		return this.applyChargePowerLimit(0);
	}
}
