package io.openems.edge.io.kmtronic;

import java.util.Optional;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(name = "IO.KMtronic", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class KmtronicRelayOutput extends AbstractOpenemsModbusComponent
		implements DigitalOutput, OpenemsComponent, ModbusSlave {

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private final BooleanWriteChannel[] digitalOutputChannels;

	public KmtronicRelayOutput() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				ThisChannelId.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(ThisChannelId.RELAY_1), //
				this.channel(ThisChannelId.RELAY_2), //
				this.channel(ThisChannelId.RELAY_3), //
				this.channel(ThisChannelId.RELAY_4), //
				this.channel(ThisChannelId.RELAY_5), //
				this.channel(ThisChannelId.RELAY_6), //
				this.channel(ThisChannelId.RELAY_7), //
				this.channel(ThisChannelId.RELAY_8), //
		};
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				/*
				 * For Read: Read Coils
				 */
				new FC1ReadCoilsTask(0, Priority.LOW, //
						m(ThisChannelId.RELAY_1, new CoilElement(0)), //
						m(ThisChannelId.RELAY_2, new CoilElement(1)), //
						m(ThisChannelId.RELAY_3, new CoilElement(2)), //
						m(ThisChannelId.RELAY_4, new CoilElement(3)), //
						m(ThisChannelId.RELAY_5, new CoilElement(4)), //
						m(ThisChannelId.RELAY_6, new CoilElement(5)), //
						m(ThisChannelId.RELAY_7, new CoilElement(6)), //
						m(ThisChannelId.RELAY_8, new CoilElement(7)) //
				),
				/*
				 * For Write: Write Single Coil
				 */
				new FC5WriteCoilTask(0, m(ThisChannelId.RELAY_1, new CoilElement(0))), //
				new FC5WriteCoilTask(1, m(ThisChannelId.RELAY_2, new CoilElement(1))), //
				new FC5WriteCoilTask(2, m(ThisChannelId.RELAY_3, new CoilElement(2))), //
				new FC5WriteCoilTask(3, m(ThisChannelId.RELAY_4, new CoilElement(3))), //
				new FC5WriteCoilTask(4, m(ThisChannelId.RELAY_5, new CoilElement(4))), //
				new FC5WriteCoilTask(5, m(ThisChannelId.RELAY_6, new CoilElement(5))), //
				new FC5WriteCoilTask(6, m(ThisChannelId.RELAY_7, new CoilElement(6))), //
				new FC5WriteCoilTask(7, m(ThisChannelId.RELAY_8, new CoilElement(7))) //
		);
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}

	@Override
	public String debugLog() {
		StringBuilder b = new StringBuilder();
		int i = 1;
		for (WriteChannel<Boolean> channel : this.digitalOutputChannels) {
			String valueText;
			Optional<Boolean> valueOpt = channel.value().asOptional();
			if (valueOpt.isPresent()) {
				valueText = valueOpt.get() ? "x" : "-";
			} else {
				valueText = "?";
			}
			b.append(i + valueText);

			// add space for all but the last
			if (++i <= this.digitalOutputChannels.length) {
				b.append(" ");
			}
		}
		return b.toString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable( //
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(KmtronicRelayOutput.class, accessMode, 100)//
						.channel(0, ThisChannelId.RELAY_1, ModbusType.UINT16) //
						.channel(1, ThisChannelId.RELAY_2, ModbusType.UINT16) //
						.channel(2, ThisChannelId.RELAY_3, ModbusType.UINT16) //
						.channel(3, ThisChannelId.RELAY_4, ModbusType.UINT16) //
						.channel(4, ThisChannelId.RELAY_5, ModbusType.UINT16) //
						.channel(5, ThisChannelId.RELAY_6, ModbusType.UINT16) //
						.channel(6, ThisChannelId.RELAY_7, ModbusType.UINT16) //
						.channel(7, ThisChannelId.RELAY_8, ModbusType.UINT16) //
						.build()//
		);
	}

}
