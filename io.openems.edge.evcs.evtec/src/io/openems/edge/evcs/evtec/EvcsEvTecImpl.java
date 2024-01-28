package io.openems.edge.evcs.evtec;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.EvcsUtils;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.ManagedVehicleBattery;
import io.openems.edge.evcs.api.SocEvcs;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Evtec", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS }) //

public class EvcsEvTecImpl extends AbstractOpenemsModbusComponent implements EvcsEvTec, Evcs, ManagedEvcs, SocEvcs,
		ManagedVehicleBattery, ModbusComponent, EventHandler, TimedataProvider, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(EvcsEvTecImpl.class);

	private Config config;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private final AccumulateEnergy calculateActiveConsumptionEnergy = new AccumulateEnergy(this,
			Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY);

	private Integer lastEnergySession = 0;

	/**
	 * Handles charge states.
	 */
	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	/**
	 * Processes the controller's writes to this evcs component.
	 */
	private final WriteHandler writeHandler = new WriteHandler(this);

	@Reference
	private EvcsPower evcsPower;

	public EvcsEvTecImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				SocEvcs.ChannelId.values(), //
				ManagedVehicleBattery.ChannelId.values(), //
				EvcsEvTec.ChannelId.values() //
		);
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this._setStatus(Status.ERROR);
		this._setFixedMinimumHardwarePower(0);
		this._setFixedMaximumHardwarePower(11_000);
		this._setMinimumPower(this.config.minHwCurrent() * 3 * Evcs.DEFAULT_VOLTAGE / 1000);
		this._setMaximumPower(this.config.maxHwCurrent() * 3 * Evcs.DEFAULT_VOLTAGE / 1000);
		this._setMaximumDischargePower(this.config.maxHwCurrentDischarge() * 3 * Evcs.DEFAULT_VOLTAGE / 1000);
		this.addListeners();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void addListeners() {

		this.getChargedEnergyChannel().onUpdate(newValueOpt -> {
			var energySession = this.lastEnergySession;
			if (newValueOpt.isDefined()) {
				energySession = newValueOpt.get().intValue();
				this.lastEnergySession = energySession;
			}
			this._setEnergySession(energySession);
		});

		// charge power = active power kept positive
		this.getActivePowerChannel().onUpdate(newValueOpt -> {
			if (newValueOpt.isDefined()) {
				var newValue = newValueOpt.get();
				this._setChargePower(Math.max(newValue, 0));
			}
		});

		// use either EVCS interface or ManagedVehicleBattery interface, depending on
		// BatteryMode

		this.getSetChargePowerLimitChannel().onSetNextWrite(value -> {
			boolean batMode = this.getBatteryMode().orElse(false);
			if (!batMode) {
				// log.info("EVCS Req (SetChargePowerLimit) " + value);

				if (value != null && value != 0) {
					value = TypeUtils.fitWithin(this.getMinimumPower().get(), this.getMaximumPower().get(), value);
				}
				this.setInputPower(value);
				this._setSetChargePowerLimit(value);
			}
		});

		this.getSetActivePowerChannel().onSetNextWrite(value -> {
			boolean batMode = this.getBatteryMode().orElse(false);
			if (batMode) {
				// log.info("VehicleBatReq (SetActivePower) " + value);

				if (value != null) {
					// negative value indicate charging of the battery, positive value indicate
					// discharge of the battery,
					if (value > 0) {
						value = TypeUtils.fitWithin(0, this.getMaximumDischargePower().get(), value);
					} else if (value < 0) {
						value = TypeUtils.fitWithin(-this.getMaximumPower().get(), -this.getMinimumPower().get(),
								value);
					}
				}
				this.setInputPower(-value);
				this.getSetActivePowerChannel().setNextValue(-value);
			} else {
				this.getSetActivePowerChannel().setNextValue(null);
			}
		});
	}

	@Override
	public void handleEvent(Event event) {

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			try {
				this.mapStatus();
				this.channel(EvcsEvTec.ChannelId.COULD_NOT_READ_CHARGING_STATE).setNextValue(false);
			} catch (OpenemsException e) {
				this.channel(EvcsEvTec.ChannelId.COULD_NOT_READ_CHARGING_STATE).setNextValue(true);
			}
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateActiveConsumptionEnergy.update(this.lastEnergySession);
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
			this.writeHandler.run();
			break;
		}
	}

	private void mapStatus() throws OpenemsException {
		var chargeStateOpt = this.channel(EvcsEvTec.ChannelId.CHARGING_STATE).value();
		if (!chargeStateOpt.isDefined()) {
			this._setStatus(Status.UNDEFINED);
			throw new OpenemsException("Could not read ChargeState");
		}

		ChargingState chargeState = chargeStateOpt.asEnum();
		switch (chargeState) {
		case NO_VEHICLE_CONNECTED:
			this._setStatus(Status.NOT_READY_FOR_CHARGING);
			this._setSoc(null); // EvTec sends 0 as Soc if no vehicle is connected
			break;
		case WAITING_FOR_RELEASE:
			this._setStatus(Status.STARTING);
			break;
		case SUSPENDED:
			this._setStatus(Status.READY_FOR_CHARGING);
			break;
		case CHARGING_PROCESS_STARTS:
			this._setStatus(Status.CHARGING);
			break;
		case STOP:
		case CHARGING_PROCESS_SUCCESSFULLY_COMPLETED:
		case CHARGING_PROCESS_COMPLETED_BY_USER:
			this._setStatus(Status.CHARGING_FINISHED);
			break;
		case CHARGING_ENDED_WITH_ERROR:
			this._setStatus(Status.ERROR);
			break;
		default:
			this._setStatus(Status.UNDEFINED);

		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		var protocol = new ModbusProtocol(this,

				new FC3ReadRegistersTask(0, Priority.HIGH,
						this.m(EvcsEvTec.ChannelId.STATION_STATE, new UnsignedWordElement(0)),
						this.m(EvcsEvTec.ChannelId.CHARGING_STATE, new UnsignedWordElement(1)),
						new DummyRegisterElement(2), this.m(EvcsEvTec.ChannelId.VOLTAGE, new FloatDoublewordElement(3)),
						this.m(EvcsEvTec.ChannelId.POWER_UINT, new UnsignedDoublewordElement(5)),
						this.m(EvcsEvTec.ChannelId.CURRENT, new FloatDoublewordElement(7)),
						this.m(ManagedVehicleBattery.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(9)),
						this.m(SocEvcs.ChannelId.SOC, new UnsignedWordElement(11),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1),
						this.m(EvcsEvTec.ChannelId.CONNECTOR_TYPE, new UnsignedWordElement(12)),
						this.m(Evcs.ChannelId.MAXIMUM_HARDWARE_POWER, new UnsignedDoublewordElement(13)),
						this.m(Evcs.ChannelId.MINIMUM_HARDWARE_POWER, new UnsignedDoublewordElement(15)),
						this.m(EvcsEvTec.ChannelId.CHARGE_TIME, new FloatDoublewordElement(17)),
						this.m(EvcsEvTec.ChannelId.CHARGED_ENERGY, new FloatDoublewordElement(19)),
						this.m(EvcsEvTec.ChannelId.DISCHARGED_ENERGY, new FloatDoublewordElement(21)),
						this.m(EvcsEvTec.ChannelId.L_LIMIT_POTENTIAL, new FloatDoublewordElement(23)),
						this.m(EvcsEvTec.ChannelId.L_LIMIT_POTENTIAL_L1, new FloatDoublewordElement(25)),
						this.m(EvcsEvTec.ChannelId.L_LIMIT_POTENTIAL_L2, new FloatDoublewordElement(27)),
						this.m(EvcsEvTec.ChannelId.L_LIMIT_POTENTIAL_L3, new FloatDoublewordElement(29)),
						this.m(EvcsEvTec.ChannelId.U_LIMIT_REQUEST, new FloatDoublewordElement(31)),
						this.m(EvcsEvTec.ChannelId.U_LIMIT_REQUEST_L1, new FloatDoublewordElement(33)),
						this.m(EvcsEvTec.ChannelId.U_LIMIT_REQUEST_L2, new FloatDoublewordElement(35)),
						this.m(EvcsEvTec.ChannelId.U_LIMIT_REQUEST_L3, new FloatDoublewordElement(37)),
						this.m(EvcsEvTec.ChannelId.L_LIMIT_REQUEST, new FloatDoublewordElement(39)),
						this.m(EvcsEvTec.ChannelId.L_LIMIT_REQUEST_L1, new FloatDoublewordElement(41)),
						this.m(EvcsEvTec.ChannelId.L_LIMIT_REQUEST_L2, new FloatDoublewordElement(43)),
						this.m(EvcsEvTec.ChannelId.L_LIMIT_REQUEST_L3, new FloatDoublewordElement(45)),
						this.m(EvcsEvTec.ChannelId.PRESENT_CONSUMPTION, new FloatDoublewordElement(47)),
						this.m(EvcsEvTec.ChannelId.PRESENT_CONSUMPTION_L1, new FloatDoublewordElement(49)),
						this.m(EvcsEvTec.ChannelId.PRESENT_CONSUMPTION_L2, new FloatDoublewordElement(51)),
						this.m(EvcsEvTec.ChannelId.PRESENT_CONSUMPTION_L3, new FloatDoublewordElement(53)),
						this.m(EvcsEvTec.ChannelId.ERROR, new UnsignedQuadruplewordElement(55)),
						this.m(EvcsEvTec.ChannelId.TOTAL_BATTERY_CAPACITY, new FloatDoublewordElement(59)),
						this.m(EvcsEvTec.ChannelId.REMAINING_BATTERY_CAPACITY, new FloatDoublewordElement(61)),
						this.m(EvcsEvTec.ChannelId.MINIMAL_BATTERY_CAPACITY, new FloatDoublewordElement(63)),
						this.m(EvcsEvTec.ChannelId.BULK_CHARGE_CAPACITY, new FloatDoublewordElement(65))),

				new FC3ReadRegistersTask(100, Priority.HIGH,
						this.m(EvcsEvTec.ChannelId.RFID, new StringWordElement(100, 20)),
						this.m(EvcsEvTec.ChannelId.EVCC_ID, new StringWordElement(120, 12))),

				new FC3ReadRegistersTask(600, Priority.HIGH,
						this.m(EvcsEvTec.ChannelId.INPUT_POWER, new SignedDoublewordElement(600)),
						this.m(EvcsEvTec.ChannelId.SUSPEND_MODE, new UnsignedWordElement(602))));

		if (!this.config.readOnly()) {
			protocol.addTask(new FC16WriteRegistersTask(600,
					this.m(EvcsEvTec.ChannelId.INPUT_POWER, new SignedDoublewordElement(600)), //
					this.m(EvcsEvTec.ChannelId.SUSPEND_MODE, new UnsignedWordElement(602))));
		}
		return protocol;
	}

	/*
	 * TODO
	 *
	 * 3) SUSPEND_MODE umstellen auf RW SUSPEND_MODE = 0 wenn controller OFF _>
	 * SUSPEND_MODE = 1 wenn controller nicht laden kann SUSPEND_MODE = 1 wenn
	 * controller nicht laden kann INFO Channel setzen 5) -handle DISCHARGED_ENERGY
	 * -handle CHARGINGSTATION_COMMUNICATION_FAILED -ausgelesener ERROR Channel
	 * sollte im Fehlerfall STATE.WARNING aktivieren
	 *
	 */

	@Override
	public String debugLog() {
		var other = ", SetActivePower: " + this.getSetActivePower() + ", SetChargePowerLimit: "
				+ this.getSetChargePowerLimit();

		return this.getStatus() + ", " + this.channel(EvcsEvTec.ChannelId.CONNECTOR_TYPE).value() + ", SOC: "
				+ this.getSoc() + ", InputPower " + this.getInputPower() + ", BatMode: " + this.getBatteryMode()
				+ other;
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return EvcsUtils.currentInMilliampereToPower(this.config.minHwCurrent(), 3);
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return EvcsUtils.currentInMilliampereToPower(this.config.maxHwCurrent(), 3);
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean pauseChargeProcess() throws Exception {
		this.getSuspendModeChannel().setNextWriteValue(true);
		return this.applyChargePowerLimit(0);
	}

	@Override
	public boolean applyDisplayText(String text) throws OpenemsException {
		return false;
	}

	@Override
	public int getMinimumTimeTillChargingLimitTaken() {
		// TODO needs to be tested
		return 4;
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

}