package io.openems.edge.system.fenecon.industrial.l;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.element.DummyCoilElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC2ReadInputsTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.system.fenecon.industrial.l.envicool.Envicool;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "System.Fenecon.Industrial.L", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class SystemFeneconIndustrialLImpl extends AbstractOpenemsModbusComponent
		implements SystemFeneconIndustrialL, ModbusComponent, OpenemsComponent, EventHandler {

	private final Envicool envicool;
	private final List<Battery> batteries = new CopyOnWriteArrayList<>();

	public SystemFeneconIndustrialLImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SystemFeneconIndustrialL.ChannelId.values() //
		);
		this.envicool = new Envicool();
	}

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setAcModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	protected synchronized void addBattery(Battery battery) {
		this.batteries.add(battery);
	}

	protected synchronized void removeBattery(Battery battery) {
		this.batteries.add(battery);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.envicool.activate(Envicool.Context.from(config, this.batteries, //
				this.getBmsModeControlModeChannel(), this.getRunTimeControlModeChannel(), //
				this.getCoolingPointChannel(), this.getHeatingPointChannel(), //
				this.getMonitorAndIssueMinTempChannel(), this.getMonitorAndIssueMaxTempChannel()));

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.acModbusUnitId(), this.cm,
				"AcModbus", config.acModbus_id())) {
			return;
		}

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Battery", config.batteryIds())) {
			return;
		}
	}

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
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.envicool.onAfterProcessImage();
			break;
		}

	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		// Envicool AC Modbus Registers
		return new ModbusProtocol(this, //
				new FC2ReadInputsTask(4, Priority.LOW, //
						m(SystemFeneconIndustrialL.ChannelId.MONITOR_SWITCH, new CoilElement(4)), //
						new DummyCoilElement(5), //
						m(SystemFeneconIndustrialL.ChannelId.COMPRESSOR_1_GENERAL_ALARM, new CoilElement(6)), //
						new DummyCoilElement(7), //
						m(SystemFeneconIndustrialL.ChannelId.PUMP_RUNNING_STATE, new CoilElement(8)), //
						new DummyCoilElement(9), new DummyCoilElement(10), new DummyCoilElement(11),
						new DummyCoilElement(12), new DummyCoilElement(13),
						m(SystemFeneconIndustrialL.ChannelId.COMPRESSOR_1_RUNNING_STATE, new CoilElement(14)), //
						new DummyCoilElement(15), new DummyCoilElement(16), new DummyCoilElement(17),
						new DummyCoilElement(18), new DummyCoilElement(19), //
						m(SystemFeneconIndustrialL.ChannelId.COMPRESSOR_2_RUNNING_STATE, new CoilElement(20))), //
				new FC2ReadInputsTask(55, Priority.LOW, //
						m(SystemFeneconIndustrialL.ChannelId.SYSTEM_WATER_SHORTAGE_ALARM, new CoilElement(55)), //
						new DummyCoilElement(56), new DummyCoilElement(57), new DummyCoilElement(58),
						new DummyCoilElement(59), new DummyCoilElement(60), new DummyCoilElement(61),
						new DummyCoilElement(62), new DummyCoilElement(63), new DummyCoilElement(64),
						new DummyCoilElement(65), new DummyCoilElement(66), new DummyCoilElement(67),
						new DummyCoilElement(68), new DummyCoilElement(69), new DummyCoilElement(70), //
						m(SystemFeneconIndustrialL.ChannelId.COMPRESSOR_1_CONDENSING_PRESSURE_TOO_HIGH_ALARM,
								new CoilElement(71)), //
						new DummyCoilElement(72), new DummyCoilElement(73), new DummyCoilElement(74),
						new DummyCoilElement(75), new DummyCoilElement(76), new DummyCoilElement(77),
						new DummyCoilElement(78), new DummyCoilElement(79), new DummyCoilElement(80),
						new DummyCoilElement(81), new DummyCoilElement(82), new DummyCoilElement(83),
						new DummyCoilElement(84), new DummyCoilElement(85), //
						m(SystemFeneconIndustrialL.ChannelId.ELECTRIC_HEATING_WORKING, new CoilElement(86))), //
				new FC2ReadInputsTask(141, Priority.LOW, //
						m(SystemFeneconIndustrialL.ChannelId.COMPRESSOR_2_CONDENSING_PRESSURE_TOO_HIGH_ALARM,
								new CoilElement(141))), //
				new FC2ReadInputsTask(178, Priority.LOW, //
						m(SystemFeneconIndustrialL.ChannelId.WATER_OUTLET_PRESSURE_HIGH_ALARM, new CoilElement(178)), //
						m(SystemFeneconIndustrialL.ChannelId.WATER_INLET_PRESSURE_WARNING, new CoilElement(179))), //
				new FC2ReadInputsTask(223, Priority.LOW, //
						m(SystemFeneconIndustrialL.ChannelId.HEATER_RUNNING_STATE, new CoilElement(223)), //
						new DummyCoilElement(224), //
						m(SystemFeneconIndustrialL.ChannelId.POWER_FAILURE_WARNING, new CoilElement(225))), //
				new FC2ReadInputsTask(391, Priority.LOW, //
						m(SystemFeneconIndustrialL.ChannelId.COMPRESSOR_2_GENERAL_ALARM, new CoilElement(391))), //

				new FC3ReadRegistersTask(1, Priority.LOW, //
						m(SystemFeneconIndustrialL.ChannelId.COOLING_POINT, new UnsignedWordElement(1), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(2, 4), //
						m(SystemFeneconIndustrialL.ChannelId.PUMP_MANUAL_DEMAND_ENABLE, new UnsignedWordElement(5)), //
						new DummyRegisterElement(6, 11), //
						m(SystemFeneconIndustrialL.ChannelId.RUNTIME_CONTROL_MODE, new UnsignedWordElement(12))), //
				new FC3ReadRegistersTask(370, Priority.LOW, //
						m(SystemFeneconIndustrialL.ChannelId.HEATING_POINT, new UnsignedWordElement(370), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(371), //
						m(SystemFeneconIndustrialL.ChannelId.WATER_PRESSURE_OUTLET_TOO_HIGH,
								new UnsignedWordElement(372)), //
						m(SystemFeneconIndustrialL.ChannelId.WATER_PRESSURE_INLET_TOO_HIGH,
								new UnsignedWordElement(373)), //
						m(SystemFeneconIndustrialL.ChannelId.PUMP_ROTATING_SPEED, new UnsignedWordElement(374), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(375), //
						m(SystemFeneconIndustrialL.ChannelId.BMS_MODE_CONTROL, new UnsignedWordElement(376)), //
						m(SystemFeneconIndustrialL.ChannelId.MONITOR_AND_ISSUE_MAX_TEMP, new UnsignedWordElement(377), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(SystemFeneconIndustrialL.ChannelId.MONITOR_AND_ISSUE_MIN_TEMP, new UnsignedWordElement(378), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //

				new FC4ReadInputRegistersTask(0, Priority.LOW, //
						m(SystemFeneconIndustrialL.ChannelId.COMPRESSOR_1_SYSTEM_FAILURE, new UnsignedWordElement(0)), //
						new DummyRegisterElement(1, 67), //
						m(SystemFeneconIndustrialL.ChannelId.WATER_OUTLET_TEMP, new UnsignedWordElement(68), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1), //
						m(SystemFeneconIndustrialL.ChannelId.WATER_INLET_TEMP, new UnsignedWordElement(69), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				new FC4ReadInputRegistersTask(172, Priority.LOW, //
						m(SystemFeneconIndustrialL.ChannelId.WATER_OUTLET_PRESSURE, new UnsignedWordElement(172)), //
						m(SystemFeneconIndustrialL.ChannelId.WATER_INLET_PRESSURE, new UnsignedWordElement(173))), //
				new FC4ReadInputRegistersTask(390, Priority.LOW, //
						m(SystemFeneconIndustrialL.ChannelId.COMPRESSOR_2_SYSTEM_FAILURE,
								new UnsignedWordElement(390))), //
				new FC5WriteCoilTask(4, //
						m(SystemFeneconIndustrialL.ChannelId.MONITOR_SWITCH, new CoilElement(4))), //
				new FC5WriteCoilTask(86, //
						m(SystemFeneconIndustrialL.ChannelId.ELECTRIC_HEATING_WORKING, new CoilElement(86))), //
				new FC6WriteRegisterTask(1, //
						m(SystemFeneconIndustrialL.ChannelId.COOLING_POINT, new UnsignedWordElement(1), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				new FC6WriteRegisterTask(5, //
						m(SystemFeneconIndustrialL.ChannelId.PUMP_MANUAL_DEMAND_ENABLE, new UnsignedWordElement(5))), //
				new FC6WriteRegisterTask(12, //
						m(SystemFeneconIndustrialL.ChannelId.RUNTIME_CONTROL_MODE, new UnsignedWordElement(12))), //
				new FC6WriteRegisterTask(370, //
						m(SystemFeneconIndustrialL.ChannelId.HEATING_POINT, new UnsignedWordElement(370), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				new FC6WriteRegisterTask(372, //
						m(SystemFeneconIndustrialL.ChannelId.WATER_PRESSURE_OUTLET_TOO_HIGH,
								new UnsignedWordElement(372))), //
				new FC6WriteRegisterTask(373, //
						m(SystemFeneconIndustrialL.ChannelId.WATER_PRESSURE_INLET_TOO_HIGH,
								new UnsignedWordElement(373))), //
				new FC6WriteRegisterTask(374, //
						m(SystemFeneconIndustrialL.ChannelId.PUMP_ROTATING_SPEED, new UnsignedWordElement(374), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				new FC6WriteRegisterTask(376, //
						m(SystemFeneconIndustrialL.ChannelId.BMS_MODE_CONTROL, new UnsignedWordElement(376))), //
				new FC6WriteRegisterTask(377, //
						m(SystemFeneconIndustrialL.ChannelId.MONITOR_AND_ISSUE_MAX_TEMP, new UnsignedWordElement(377), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //
				new FC6WriteRegisterTask(378, //
						m(SystemFeneconIndustrialL.ChannelId.MONITOR_AND_ISSUE_MIN_TEMP, new UnsignedWordElement(378), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)) //
		);
	}

}
