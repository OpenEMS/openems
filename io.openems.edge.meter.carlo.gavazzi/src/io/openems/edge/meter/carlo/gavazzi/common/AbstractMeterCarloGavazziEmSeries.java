package io.openems.edge.meter.carlo.gavazzi.common;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

public abstract class AbstractMeterCarloGavazziEmSeries extends AbstractOpenemsModbusComponent
		implements MeterCarloGavazziEmSeries, SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent {
	
	protected ModbusProtocol modbusProtocol;

	public AbstractMeterCarloGavazziEmSeries(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
	}

	@Activate
	protected
	void activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId, boolean invert) throws OpenemsException {

		this.setModbusProtocol(invert);

		if (super.activate(context, id, alias, enabled, unitId, cm,
				modbusReference, modbusId)) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	

	protected void setModbusProtocol(boolean invert) throws OpenemsException {
		final var offset = 300000 + 1;
		/**
		 * See Modbus definition PDF-file in doc directory and
		 * https://www.galoz.co.il/wp-content/uploads/2014/11/EM341-Modbus.pdf
		 */

		this.modbusProtocol = new ModbusProtocol(this, //
				new FC4ReadInputRegistersTask(300001 - offset, Priority.LOW, //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L1,
								new SignedDoublewordElement(300001 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2,
								new SignedDoublewordElement(300003 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3,
								new SignedDoublewordElement(300005 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2)),

				new FC4ReadInputRegistersTask(300013 - offset, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.CURRENT_L1,
								new SignedDoublewordElement(300013 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2_AND_INVERT_IF_TRUE(invert)),
						m(AsymmetricMeter.ChannelId.CURRENT_L2,
								new SignedDoublewordElement(300015 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2_AND_INVERT_IF_TRUE(invert)),
						m(AsymmetricMeter.ChannelId.CURRENT_L3,
								new SignedDoublewordElement(300017 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter.SCALE_FACTOR_2_AND_INVERT_IF_TRUE(invert))),

				// NOTE: EM 24 can only read a limited number of registers at once
				new FC4ReadInputRegistersTask(300019 - offset, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1,
								new SignedDoublewordElement(300019 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(invert)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2,
								new SignedDoublewordElement(300021 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(invert)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3,
								new SignedDoublewordElement(300023 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(invert))),

				// NOTE: EM 24 can only read a limited number of registers at once
				new FC4ReadInputRegistersTask(300025 - offset, Priority.HIGH, //
						m(MeterCarloGavazziEmSeries.ChannelId.APPARENT_POWER_L1,
								new SignedDoublewordElement(300025 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(invert)),
						m(MeterCarloGavazziEmSeries.ChannelId.APPARENT_POWER_L2,
								new SignedDoublewordElement(300027 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(invert)),
						m(MeterCarloGavazziEmSeries.ChannelId.APPARENT_POWER_L3,
								new SignedDoublewordElement(300029 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(invert))),

				// NOTE: EM 24 can only read a limited number of registers at once
				new FC4ReadInputRegistersTask(300031 - offset, Priority.HIGH, //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1,
								new SignedDoublewordElement(300031 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(invert)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2,
								new SignedDoublewordElement(300033 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(invert)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3,
								new SignedDoublewordElement(300035 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(invert))),

				// NOTE: EM 24 can only read a limited number of registers at once
				new FC4ReadInputRegistersTask(300041 - offset, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER,
								new SignedDoublewordElement(300041 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(invert)),
						m(MeterCarloGavazziEmSeries.ChannelId.APPARENT_POWER,
								new SignedDoublewordElement(300043 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(invert)),
						m(SymmetricMeter.ChannelId.REACTIVE_POWER,
								new SignedDoublewordElement(300045 - offset).wordOrder(WordOrder.LSWMSW),
								ElementToChannelConverter
										.SCALE_FACTOR_MINUS_1_AND_INVERT_IF_TRUE(invert))));

	}

}
