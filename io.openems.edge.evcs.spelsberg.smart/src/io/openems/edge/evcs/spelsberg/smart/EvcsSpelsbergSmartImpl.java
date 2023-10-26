package io.openems.edge.evcs.spelsberg.smart;

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
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.ChargeStateHandler;
import io.openems.edge.evcs.api.ChargingType;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Phases;
import io.openems.edge.evcs.api.Status;
import io.openems.edge.evcs.api.WriteHandler;

@Designate(ocd = Config.class, factory = true)
@Component(name = "EVCS.Spelsberg.SMART", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
@EventTopics({ EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE })
public class EvcsSpelsbergSmartImpl extends AbstractOpenemsModbusComponent
		implements EvcsSpelsbergSmart, Evcs, ManagedEvcs, ModbusComponent, OpenemsComponent, EventHandler {

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private EvcsPower evcsPower;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config = null;

	private final Logger log = LoggerFactory.getLogger(EvcsSpelsbergSmartImpl.class);

	private final ChargeStateHandler chargeStateHandler = new ChargeStateHandler(this);

	private final WriteHandler writeHandler = new WriteHandler(this);

	public EvcsSpelsbergSmartImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), ModbusComponent.ChannelId.values(), Evcs.ChannelId.values(),
				ManagedEvcs.ChannelId.values(), EvcsSpelsbergSmart.ChannelId.values());
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		/*
		 * Calculates the maximum and minimum hardware power dynamically by listening on
		 * the fixed hardware limits used for charging
		 */
		Evcs.addCalculatePowerLimitListeners(this);
		applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		applyConfig(config);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		var modbusProtocol = new ModbusProtocol(this,
				new FC3ReadRegistersTask(1000, Priority.HIGH,
						m(EvcsSpelsbergSmart.ChannelId.CHARGE_POINT_STATE, new UnsignedWordElement(1000)),
						new DummyRegisterElement(1001),
						m(EvcsSpelsbergSmart.ChannelId.EVSE_STATE, new UnsignedWordElement(1002))),

				new FC3ReadRegistersTask(1004, Priority.LOW,
						m(EvcsSpelsbergSmart.ChannelId.CABLE_STATE, new UnsignedWordElement(1004))),

				new FC3ReadRegistersTask(1008, Priority.LOW,
						m(EvcsSpelsbergSmart.ChannelId.CURRENT_L1, new UnsignedWordElement(1008))),

				new FC3ReadRegistersTask(1010, Priority.LOW,
						m(EvcsSpelsbergSmart.ChannelId.CURRENT_L2, new UnsignedWordElement(1010))),

				new FC3ReadRegistersTask(1012, Priority.LOW,
						m(EvcsSpelsbergSmart.ChannelId.CURRENT_L3, new UnsignedWordElement(1012))),

				new FC3ReadRegistersTask(1020, Priority.HIGH,
						m(Evcs.ChannelId.CHARGE_POWER, new UnsignedDoublewordElement(1020))),

				new FC3ReadRegistersTask(1020, Priority.HIGH,
						m(EvcsSpelsbergSmart.ChannelId.POWER_TOTAL, new UnsignedDoublewordElement(1020))),

				new FC3ReadRegistersTask(1024, Priority.HIGH,
						m(EvcsSpelsbergSmart.ChannelId.POWER_L1, new UnsignedDoublewordElement(1024))),

				new FC3ReadRegistersTask(1028, Priority.HIGH,
						m(EvcsSpelsbergSmart.ChannelId.POWER_L2, new UnsignedDoublewordElement(1028))),

				new FC3ReadRegistersTask(1032, Priority.HIGH,
						m(EvcsSpelsbergSmart.ChannelId.POWER_L3, new UnsignedDoublewordElement(1032))),

				new FC3ReadRegistersTask(1036, Priority.LOW,
						m(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(1036))),

				new FC3ReadRegistersTask(1100, Priority.LOW,
						m(EvcsSpelsbergSmart.ChannelId.MAX_HARDWARE_CURRENT, new UnsignedWordElement(1100))),

				new FC3ReadRegistersTask(1102, Priority.LOW,
						m(EvcsSpelsbergSmart.ChannelId.MIN_HARDWARE_CURRENT, new UnsignedWordElement(1102))),

				new FC3ReadRegistersTask(1502, Priority.HIGH,
						m(EvcsSpelsbergSmart.ChannelId.CHARGE_ENERGY_SESSION, new UnsignedWordElement(1502))),

				new FC3ReadRegistersTask(1504, Priority.LOW,
						m(EvcsSpelsbergSmart.ChannelId.CHARGE_START_TIME, new UnsignedDoublewordElement(1504))),

				new FC3ReadRegistersTask(1508, Priority.LOW,
						m(EvcsSpelsbergSmart.ChannelId.CHARGE_DURATION_SESSION, new UnsignedDoublewordElement(1508))),

				new FC3ReadRegistersTask(1512, Priority.LOW,
						m(EvcsSpelsbergSmart.ChannelId.CHARGE_STOP_TIME, new UnsignedDoublewordElement(1512))),

				new FC3ReadRegistersTask(2000, Priority.LOW,
						m(EvcsSpelsbergSmart.ChannelId.CHARGE_SAVE_CURRENT_LIMIT, new UnsignedWordElement(2000))),

				new FC3ReadRegistersTask(6000, Priority.HIGH,
						m(EvcsSpelsbergSmart.ChannelId.LIFE_BIT, new UnsignedWordElement(6000))),

				new FC6WriteRegisterTask(6000, m(EvcsSpelsbergSmart.ChannelId.LIFE_BIT, new UnsignedWordElement(6000))),

				new FC16WriteRegistersTask(5000,
						m(EvcsSpelsbergSmart.ChannelId.APPLY_CHARGE_POWER_LIMIT, new UnsignedDoublewordElement(5000))),

				new FC6WriteRegisterTask(5004,
						m(EvcsSpelsbergSmart.ChannelId.APPLY_CHARGE_CURRENT_LIMIT, new UnsignedWordElement(5004))));

		addStatusCallback();
		addPhaseDetectionCallback();
		addPowerConsumptionCallback();
		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		return "Status: " + getStatus().getName() + " | " + "Charge Power Limit: "
				+ getSetChargePowerLimit().orElse(null) + " | " + "Actual Charge Power: " + getChargePowerTotal()
				+ " | " + "Charged Energy: " + getEnergySessionChannel().value();
	}

	@Override
	public void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

	@Override
	public int getConfiguredMinimumHardwarePower() {
		return currentToPower(this.config.minHwCurrent());
	}

	@Override
	public int getConfiguredMaximumHardwarePower() {
		return currentToPower(this.config.maxHwCurrent());
	}

	@Override
	public boolean getConfiguredDebugMode() {
		return this.config.debugMode();
	}

	@Override
	public boolean applyChargePowerLimit(int power) throws Exception {
		setApplyChargePowerLimit(power);
		return true;
	}

	@Override
	public boolean pauseChargeProcess() throws Exception {
		this.applyChargePowerLimit(0);
		return true;
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
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			writeHandler.run();
			break;
		}
	}

	private void applyConfig(Config config) {
		this.config = config;
		this._setChargingType(ChargingType.AC);
		this._setPhases(Phases.THREE_PHASE.getValue());
		this._setFixedMinimumHardwarePower(currentToPower(config.minHwCurrent()));
		this._setFixedMaximumHardwarePower(currentToPower(config.maxHwCurrent()));
		this._setPowerPrecision(DEFAULT_POWER_RECISION); // 1A steps
	}

	private Integer currentToPower(Integer current) {
		return Math.round(current / 1000f) * DEFAULT_VOLTAGE * getPhasesAsInt();
	}

	private void addStatusCallback() {
		this.channel(EvcsSpelsbergSmart.ChannelId.CHARGE_POINT_STATE).onSetNextValue(s -> {
			ChargePointState state = s.asEnum();

			/**
			 * Maps the EVCS state to a {@link Status}.
			 */
			switch (state) {
			case CHARGING:
				this._setStatus(Status.CHARGING);
				break;
			case NO_PERMISSION:
			case CHARGING_STATION_RESERVED:
				this._setStatus(Status.CHARGING_REJECTED);
				break;
			case ERROR:
				this._setStatus(Status.ERROR);
				break;
			case NO_VEHICLE_ATTACHED:
				this._setStatus(Status.NOT_READY_FOR_CHARGING);
				break;
			case CHARGING_PAUSED:
				this._setStatus(Status.READY_FOR_CHARGING);
				break;
			case UNDEFINED:
			default:
				this._setStatus(Status.UNDEFINED);
			}

			if (getLifeBit() == -1) {
				this._setModbusCommunicationFailed(true);
			} else {
				this._setModbusCommunicationFailed(false);
			}
		});
	}

	/*
	 * Handle automatic phase shift and reset the fixed hardware power limits
	 */
	private void addPhaseDetectionCallback() {
		final Consumer<Value<Integer>> setPhasesCallback = ignore -> {

			var phases = 0;
			if (getChargePowerL1() > 0)
				phases++;
			if (getChargePowerL2() > 0)
				phases++;
			if (getChargePowerL3() > 0)
				phases++;

			this._setPhases(phases);

			// Set full power range from 1,38kW to 11kW
			this._setFixedMinimumHardwarePower(
					Math.round(this.config.minHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.ONE_PHASE.getValue());
			this._setFixedMaximumHardwarePower(
					Math.round(this.config.maxHwCurrent() / 1000f) * DEFAULT_VOLTAGE * Phases.THREE_PHASE.getValue());
		};

		this.getChargePowerTotalChannel().onUpdate(setPhasesCallback);
	}

	private void addPowerConsumptionCallback() {
		final Consumer<Value<Integer>> setEnergyConsumptionCallback = ignore -> {

			this._setEnergySession(getChargedEnergy());
		};

		this.getChargedEnergyChannel().onUpdate(setEnergyConsumptionCallback);
	}
}
