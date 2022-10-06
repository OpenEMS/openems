package io.openems.edge.evcs.alfen;

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
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.FloatQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.EvcsPower;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evcs.api.Status;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Evcs.Alfen", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
		} //
)
public class AlfenImpl extends AbstractOpenemsModbusComponent
		implements Alfen, Evcs, ManagedEvcs, ModbusComponent, EventHandler, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AlfenImpl.class);

	private static final float DETECT_PHASE_ACTIVITY = 400; // milliAmpere

	private Long energyAtSessionStart = 0L;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private EvcsPower evcsPower;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private int phasePattern = 0;

	public AlfenImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Evcs.ChannelId.values(), //
				ManagedEvcs.ChannelId.values(), //
				Alfen.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		this._setMaximumHardwarePower(22000);
		this._setMinimumHardwarePower(4000);

		this.installEvcsHandlers();

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void installEvcsHandlers() {
		this.channel(Alfen.ChannelId.CURRENT_L1).onUpdate((newValue) -> {
			if (newValue.isDefined()) {
				var current = (Integer) newValue.get();
				if (current.intValue() > DETECT_PHASE_ACTIVITY) {
					this.phasePattern |= 0x01;
				} else {
					this.phasePattern &= ~0x01;
				}
			} else {
				this.phasePattern &= ~0x01;
			}
			this.updatePhases();
		});
		this.channel(Alfen.ChannelId.CURRENT_L2).onUpdate((newValue) -> {
			if (newValue.isDefined()) {
				var current = (Integer) newValue.get();
				if (current.intValue() > DETECT_PHASE_ACTIVITY) {
					this.phasePattern |= 0x02;
				} else {
					this.phasePattern &= ~0x02;
				}
			} else {
				this.phasePattern &= ~0x02;
			}
			this.updatePhases();
		});
		this.channel(Alfen.ChannelId.CURRENT_L3).onUpdate((newValue) -> {
			if (newValue.isDefined()) {
				var current = (Integer) newValue.get();
				if (current.intValue() > DETECT_PHASE_ACTIVITY) {
					this.phasePattern |= 0x04;
				} else {
					this.phasePattern &= ~0x04;
				}
			} else {
				this.phasePattern &= ~0x04;
			}
			this.updatePhases();
		});

		this.channel(Alfen.ChannelId.MODE_3_STATE).onChange((oldValue, newValue) -> {
			if (!oldValue.isDefined() || !newValue.isDefined()) {
				return;
			}
			var oldState = (String) oldValue.get();
			var newState = (String) newValue.get();
			if (this.vehicleConnected(newState) && !this.vehicleConnected(oldState)) {
				this.energyAtSessionStart = this.getActiveConsumptionEnergy().get();
			}
		});

	}

	private void sendPowerLimit() {
		var reqPower = this.getSetChargePowerLimitChannel().getNextWriteValue().orElse(0);
		var minPower = this.getMinimumHardwarePower().orElse(0);
		var maxPower = this.getMaximumHardwarePower().orElse(0);
		var allowedPower = reqPower;
		if (allowedPower < minPower) {
			allowedPower = 0;
		}
		if (allowedPower > maxPower) {
			allowedPower = maxPower;
		}
		this._setSetChargePowerLimit(reqPower);

		this.setCurrentFromPower(allowedPower);
	}

	private void setCurrentFromPower(int power) {
		var current = Long.valueOf(Math.round((power * 1000 / 230.0) / 3)).intValue();

		this.transmitChargingCurrent(current);
	}

	private void transmitChargingCurrent(int current) {

		var floatCurrent = (float) (current) / 1000;
		try {
			this.setModbusSlaveMaxCurrent(floatCurrent);
			this.setModbusSlaveMaxCurrentWriteValue(floatCurrent);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Could not set charging current." + e.getMessage());
			e.printStackTrace();
		}

	}

	private void updatePhases() {
		var bitCount = Integer.bitCount(this.phasePattern);
		this._setPhases(bitCount);
	}

	private boolean vehicleConnected(String mode3State) {
		return (mode3State.length() == 2);
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.sendPowerLimit();
			this.convertStatus();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateSessionEnergy();
			this.updateCommunicationState();
			break;
		}
	}

	private void convertStatus() {

		Value<?> mode3StateVal = this.channel(Alfen.ChannelId.MODE_3_STATE).getNextValue();
		if (!mode3StateVal.isDefined()) {
			this._setStatus(Status.UNDEFINED);
			return;
		}

		String mode3State = (String) mode3StateVal.get();

		if (mode3State.startsWith("A") || mode3State.startsWith("E")) {
			this._setStatus(Status.NOT_READY_FOR_CHARGING);
			return;
		}
		if (mode3State.startsWith("B")) {
			this._setStatus(Status.READY_FOR_CHARGING);
			return;
		}
		if (mode3State.endsWith("1")) {
			this._setStatus(Status.CHARGING_FINISHED);
			return;
		}
		if (mode3State.endsWith("2")) {
			this._setStatus(Status.CHARGING);
			return;
		}
		if (mode3State.equals("F")) {
			this._setStatus(Status.ERROR);
			this.channel(Alfen.ChannelId.ERROR).setNextValue(Level.FAULT);
			return;
		}

		this._setStatus(Status.UNDEFINED);

	}

	private void calculateSessionEnergy() {
		var sessionEnergy = (int) (this.getActiveConsumptionEnergy().orElse(0L) - this.energyAtSessionStart);
		this._setEnergySession(sessionEnergy);
	}

	private void updateCommunicationState() {
		Value<Boolean> stateOpt = this.getModbusCommunicationFailed();
		if (stateOpt.isDefined()) {
			if (stateOpt.get().booleanValue()) {
				this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(Level.FAULT);
			} else {
				this.channel(Evcs.ChannelId.CHARGINGSTATION_COMMUNICATION_FAILED).setNextValue(Level.OK);
			}
		}
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {

		return new ModbusProtocol(this,

				new FC3ReadRegistersTask(300, Priority.HIGH,
						m(Alfen.ChannelId.METER_STATE, new UnsignedWordElement(300)),
						m(Alfen.ChannelId.METER_LAST_VALUE_TIMESTAMP, new UnsignedQuadruplewordElement(301)),
						m(Alfen.ChannelId.METER_TYPE, new UnsignedWordElement(305)),
						m(Alfen.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(306)),
						m(Alfen.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(308)),
						m(Alfen.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(310)),
						m(Alfen.ChannelId.VOLTAGE_L1_L2, new FloatDoublewordElement(312)),
						m(Alfen.ChannelId.VOLTAGE_L2_L3, new FloatDoublewordElement(314)),
						m(Alfen.ChannelId.VOLTAGE_L3_L1, new FloatDoublewordElement(316)),
						m(Alfen.ChannelId.CURRENT_N, new FloatDoublewordElement(318)),
						m(Alfen.ChannelId.CURRENT_L1, new FloatDoublewordElement(320)),
						m(Alfen.ChannelId.CURRENT_L2, new FloatDoublewordElement(322)),
						m(Alfen.ChannelId.CURRENT_L3, new FloatDoublewordElement(324)),
						m(Alfen.ChannelId.CURRENT, new FloatDoublewordElement(326)),
						m(Alfen.ChannelId.POWER_FACTOR_L1, new FloatDoublewordElement(328)),
						m(Alfen.ChannelId.POWER_FACTOR_L2, new FloatDoublewordElement(330)),
						m(Alfen.ChannelId.POWER_FACTOR_L3, new FloatDoublewordElement(332)),
						m(Alfen.ChannelId.POWER_FACTOR_SUM, new FloatDoublewordElement(334)),
						m(Alfen.ChannelId.FREQUENCY, new FloatDoublewordElement(336)),
						m(Alfen.ChannelId.CHARGE_POWER_L1, new FloatDoublewordElement(338)),
						m(Alfen.ChannelId.CHARGE_POWER_L2, new FloatDoublewordElement(340)),
						m(Alfen.ChannelId.CHARGE_POWER_L3, new FloatDoublewordElement(342)),
						m(Evcs.ChannelId.CHARGE_POWER, new FloatDoublewordElement(344)),
						m(Alfen.ChannelId.APPARENT_POWER_L1, new FloatDoublewordElement(346)),
						m(Alfen.ChannelId.APPARENT_POWER_L2, new FloatDoublewordElement(348)),
						m(Alfen.ChannelId.APPARENT_POWER_L3, new FloatDoublewordElement(350)),
						m(Alfen.ChannelId.APPARENT_POWER_SUM, new FloatDoublewordElement(352)),
						m(Alfen.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(354)),
						m(Alfen.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(356)),
						m(Alfen.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(358)),
						m(Alfen.ChannelId.REACTIVE_POWER_SUM, new FloatDoublewordElement(360))),

				new FC3ReadRegistersTask(374, Priority.LOW,
						m(Evcs.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatQuadruplewordElement(374))),

				new FC3ReadRegistersTask(1200, Priority.HIGH,
						m(Alfen.ChannelId.AVAILABILITY, new UnsignedWordElement(1200)),
						m(Alfen.ChannelId.MODE_3_STATE, new StringWordElement(1201, 5)),
						m(Alfen.ChannelId.ACTUAL_APPLIED_MAX_CURRENT, new FloatDoublewordElement(1206)),
						m(Alfen.ChannelId.MODBUS_SLAVE_MAX_CURRENT_VALID_TIME, new UnsignedDoublewordElement(1208)),
						m(Alfen.ChannelId.MODBUS_SLAVE_MAX_CURRENT, new FloatDoublewordElement(1210)),
						m(Alfen.ChannelId.ACTIVE_LOAD_BALANCING_SAFE_CURRENT, new FloatDoublewordElement(1212)),
						m(Alfen.ChannelId.MODBUS_SLAVE_RECEIVED_SETPOINT_ACCOUNTED_FOR, new UnsignedWordElement(1214)),
						m(Alfen.ChannelId.CHARGE_USING_1_OR_3_PHASES, new UnsignedWordElement(1215))),

				new FC16WriteRegistersTask(1210,
						m(Alfen.ChannelId.MODBUS_SLAVE_MAX_CURRENT, new FloatDoublewordElement(1210)))

		);

	}

	@Override
	public String debugLog() {
		return this.getState() + "," + this.getStatus() + "," + this.getChargePower();
	}

	@Override
	public EvcsPower getEvcsPower() {
		return this.evcsPower;
	}

}
