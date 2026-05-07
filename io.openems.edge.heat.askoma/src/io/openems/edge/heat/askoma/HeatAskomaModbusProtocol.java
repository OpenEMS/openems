package io.openems.edge.heat.askoma;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.DIRECT_1_TO_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;

import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.heat.api.Heat;
import io.openems.edge.heat.api.ManagedHeatElement;
import io.openems.edge.meter.api.ElectricityMeter;

final class HeatAskomaModbusProtocol {

	private HeatAskomaModbusProtocol() {
	}

	static ModbusProtocol define(HeatAskomaImpl parent, Config config) {
		var protocol = new ModbusProtocol(parent, //
				new FC4ReadInputRegistersTask(109, Priority.HIGH, //
						bits(parent), //
						map(parent, ElectricityMeter.ChannelId.ACTIVE_POWER, new UnsignedWordElement(110))),

				new FC3ReadRegistersTask(597, Priority.LOW, //
						map(parent, HeatAskoma.ChannelId.TEMPERATURE_SETPOINT, new UnsignedWordElement(597),
								SCALE_FACTOR_1)),

				new FC4ReadInputRegistersTask(638, Priority.HIGH, //
						map(parent, Heat.ChannelId.TEMPERATURE, new UnsignedWordElement(638), SCALE_FACTOR_1))); //

		if (!config.readOnly()) {
			// Askoma spec: MODBUS_CMD_LOAD_FEEDIN_VALUE, signed int16, -30000..30000 W
			// values are negativ therefore the TARGET_GRID_ACTIVE_POWER is used instead of
			// TARGET_ACTIVE_POWER
			protocol.addTask(new FC3ReadRegistersTask(202, Priority.HIGH, //
					map(parent, ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, new SignedWordElement(202))));

			protocol.addTask(new FC6WriteRegisterTask(202, //
					map(parent, ManagedHeatElement.ChannelId.TARGET_GRID_ACTIVE_POWER, new SignedWordElement(202))));
		}

		return protocol;
	}

	private static BitsWordElement bits(HeatAskomaImpl parent) {
		return new BitsWordElement(109, parent) //
				.bit(0, HeatAskoma.ChannelId.HEATER1_ACTIVE) //
				.bit(1, HeatAskoma.ChannelId.HEATER2_ACTIVE) //
				.bit(2, HeatAskoma.ChannelId.HEATER3_ACTIVE) //
				.bit(3, HeatAskoma.ChannelId.PUMP_ACTIVE) //
				.bit(4, HeatAskoma.ChannelId.RELAYBOARD_IS_CONNECTED) //
				.bit(5, HeatAskoma.ChannelId.HEATER_1_2_3_CURRENT_FLOW) //
				.bit(6, HeatAskoma.ChannelId.HEAT_PUMP_REQUEST_ACTIVE) //
				.bit(7, HeatAskoma.ChannelId.EMERGENCY_MODE_ACTIVE) //
				.bit(8, HeatAskoma.ChannelId.LEGIONELLA_PROTECTION_ACTIVE) //
				.bit(9, HeatAskoma.ChannelId.ANALOG_INPUT_ACTIVE) //
				.bit(10, HeatAskoma.ChannelId.LOAD_SETPOINT_ACTIVE) //
				.bit(11, HeatAskoma.ChannelId.LOAD_FEEDIN_ACTIVE) //
				.bit(12, HeatAskoma.ChannelId.AUTO_HEATER_OFF_ACTIVE) //
				.bit(13, HeatAskoma.ChannelId.PUMP_RELAY_FOLLOW_UP_ACTIVE) //
				.bit(14, HeatAskoma.ChannelId.TEMPERATURE_LIMIT_REACHED) //
				.bit(15, HeatAskoma.ChannelId.ANY_ERROR_OCCURRED);
	}

	private static <T extends ModbusElement> T map(HeatAskomaImpl parent, ChannelId channelId, T element) {
		return map(parent, channelId, element, DIRECT_1_TO_1);
	}

	private static <T extends ModbusElement> T map(HeatAskomaImpl parent, ChannelId channelId, T element,
			ElementToChannelConverter converter) {
		return parent.new ChannelMapper<T>(element) //
				.m(channelId, converter) //
				.build();
	}
}
