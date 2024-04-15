package io.openems.edge.ess.sma.stpxx3se.battery;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;

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
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.sma.enums.SetControlMode;
import io.openems.edge.ess.sma.sunnyisland.EssSmaSunnyIsland;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Ess.SMA.STP-SE.Battery", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SmaBatteryImpl extends AbstractOpenemsModbusComponent
		implements Battery, SmaBattery, ModbusComponent, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public SmaBatteryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Battery.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SmaBattery.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this._setChargeMaxVoltage(config.chargeMaxVoltage());
		this._setDischargeMinVoltage(config.dischargeMinVoltage());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(30843, Priority.LOW, //
						m(Battery.ChannelId.CURRENT, new SignedDoublewordElement(30843), //
								SCALE_FACTOR_MINUS_3),
						m(Battery.ChannelId.SOC, new UnsignedDoublewordElement(30845)),
						new DummyRegisterElement(30847, 30850), //
						m(Battery.ChannelId.VOLTAGE, new UnsignedDoublewordElement(30851),
								SCALE_FACTOR_MINUS_2), //
						new DummyRegisterElement(30853, 30954), //
						m(SmaBattery.ChannelId.BAT_STATUS, new UnsignedDoublewordElement(30955)) //
				), //
				new FC3ReadRegistersTask(32251, Priority.LOW, //
						m(Battery.ChannelId.CHARGE_MAX_CURRENT, new UnsignedDoublewordElement(32251), //
								SCALE_FACTOR_MINUS_3), //
						new DummyRegisterElement(32253, 32256), //
						m(Battery.ChannelId.DISCHARGE_MAX_CURRENT, new UnsignedDoublewordElement(32257), //
								SCALE_FACTOR_MINUS_3) //
				), //
				new FC3ReadRegistersTask(40187, Priority.LOW, //
						m(Battery.ChannelId.CAPACITY, new UnsignedDoublewordElement(40187))), //
				
				new FC16WriteRegistersTask(40149, //
						m(SmaBattery.ChannelId.SET_ACTIVE_POWER, new SignedDoublewordElement(40149)), //
						m(SmaBattery.ChannelId.SET_CONTROL_MODE, new UnsignedDoublewordElement(40151)), //
						m(SmaBattery.ChannelId.SET_REACTIVE_POWER, new SignedDoublewordElement(40153))) //
		);

	}
	
	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {

		EnumWriteChannel setControlMode = this.channel(EssSmaSunnyIsland.ChannelId.SET_CONTROL_MODE);
		IntegerWriteChannel setActivePowerChannel = this.channel(EssSmaSunnyIsland.ChannelId.SET_ACTIVE_POWER);
		IntegerWriteChannel setReactivePowerChannel = this.channel(EssSmaSunnyIsland.ChannelId.SET_REACTIVE_POWER);

		setControlMode.setNextWriteValue(SetControlMode.START);
		setActivePowerChannel.setNextWriteValue(activePower);
		setReactivePowerChannel.setNextWriteValue(reactivePower);
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		this._setStartStop(value);
	}

}
