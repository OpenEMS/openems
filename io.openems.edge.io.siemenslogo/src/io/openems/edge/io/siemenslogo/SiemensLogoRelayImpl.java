package io.openems.edge.io.siemenslogo;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.channel.AccessMode.READ_WRITE;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;

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
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Siemens.LOGO", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SiemensLogoRelayImpl extends AbstractOpenemsModbusComponent
		implements SiemensLogoRelay, DigitalOutput, DigitalInput, ModbusComponent, OpenemsComponent, ModbusSlave {

	private final BooleanWriteChannel[] digitalOutputChannels;
	private final BooleanReadChannel[] digitalInputChannels;

	private int writeOffset = 0;
	private int readOffset = 0;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public SiemensLogoRelayImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				DigitalInput.ChannelId.values(), //
				SiemensLogoRelay.ChannelId.values() //
		);
		this.digitalOutputChannels = stream(SiemensLogoRelay.ChannelId.values()) //
				.filter(channelId -> channelId.doc().getAccessMode() == READ_WRITE) //
				.map(channelId -> this.channel(channelId)) //
				.toArray(BooleanWriteChannel[]::new);
		this.digitalInputChannels = stream(SiemensLogoRelay.ChannelId.values()) //
				.filter(channelId -> channelId.doc().getAccessMode() == READ_ONLY) //
				.map(channelId -> this.channel(channelId)) //
				.toArray(BooleanReadChannel[]::new);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.writeOffset = config.modbusOffsetWriteAddress();
		this.readOffset = config.modbusOffsetReadAddress();
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}

	@Override
	public BooleanReadChannel[] digitalInputChannels() {
		return this.digitalInputChannels;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				// Read Inputs
				new FC1ReadCoilsTask(this.readOffset, Priority.HIGH, //
						m(SiemensLogoRelay.ChannelId.INPUT_1, new CoilElement(0 + this.readOffset)), //
						m(SiemensLogoRelay.ChannelId.INPUT_2, new CoilElement(1 + this.readOffset)), //
						m(SiemensLogoRelay.ChannelId.INPUT_3, new CoilElement(2 + this.readOffset)), //
						m(SiemensLogoRelay.ChannelId.INPUT_4, new CoilElement(3 + this.readOffset)) //
				),

				/*
				 * For Read: Read Coils
				 */
				new FC1ReadCoilsTask(this.writeOffset, Priority.LOW, //
						m(SiemensLogoRelay.ChannelId.RELAY_1, new CoilElement(0 + this.writeOffset)), //
						m(SiemensLogoRelay.ChannelId.RELAY_2, new CoilElement(1 + this.writeOffset)), //
						m(SiemensLogoRelay.ChannelId.RELAY_3, new CoilElement(2 + this.writeOffset)), //
						m(SiemensLogoRelay.ChannelId.RELAY_4, new CoilElement(3 + this.writeOffset)), //
						m(SiemensLogoRelay.ChannelId.RELAY_5, new CoilElement(4 + this.writeOffset)), //
						m(SiemensLogoRelay.ChannelId.RELAY_6, new CoilElement(5 + this.writeOffset)), //
						m(SiemensLogoRelay.ChannelId.RELAY_7, new CoilElement(6 + this.writeOffset)), //
						m(SiemensLogoRelay.ChannelId.RELAY_8, new CoilElement(7 + this.writeOffset)) //
				),
				/*
				 * For Write: Write Single Coil
				 */
				new FC5WriteCoilTask(0 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.RELAY_1, new CoilElement(0 + this.writeOffset))), //
				new FC5WriteCoilTask(1 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.RELAY_2, new CoilElement(1 + this.writeOffset))), //
				new FC5WriteCoilTask(2 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.RELAY_3, new CoilElement(2 + this.writeOffset))), //
				new FC5WriteCoilTask(3 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.RELAY_4, new CoilElement(3 + this.writeOffset))), //
				new FC5WriteCoilTask(4 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.RELAY_5, new CoilElement(4 + this.writeOffset))), //
				new FC5WriteCoilTask(5 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.RELAY_6, new CoilElement(5 + this.writeOffset))), //
				new FC5WriteCoilTask(6 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.RELAY_7, new CoilElement(6 + this.writeOffset))), //
				new FC5WriteCoilTask(7 + this.writeOffset,
						m(SiemensLogoRelay.ChannelId.RELAY_8, new CoilElement(7 + this.writeOffset))) //
		);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(SiemensLogoRelay.class, accessMode, 100)//
						.channel(0 + this.writeOffset, SiemensLogoRelay.ChannelId.RELAY_1, ModbusType.UINT16) //
						.channel(1 + this.writeOffset, SiemensLogoRelay.ChannelId.RELAY_2, ModbusType.UINT16) //
						.channel(2 + this.writeOffset, SiemensLogoRelay.ChannelId.RELAY_3, ModbusType.UINT16) //
						.channel(3 + this.writeOffset, SiemensLogoRelay.ChannelId.RELAY_4, ModbusType.UINT16) //
						.channel(4 + this.writeOffset, SiemensLogoRelay.ChannelId.RELAY_5, ModbusType.UINT16) //
						.channel(5 + this.writeOffset, SiemensLogoRelay.ChannelId.RELAY_6, ModbusType.UINT16) //
						.channel(6 + this.writeOffset, SiemensLogoRelay.ChannelId.RELAY_7, ModbusType.UINT16) //
						.channel(7 + this.writeOffset, SiemensLogoRelay.ChannelId.RELAY_8, ModbusType.UINT16) //

						.channel(8 + this.readOffset, SiemensLogoRelay.ChannelId.INPUT_1, ModbusType.UINT16) //
						.channel(9 + this.readOffset, SiemensLogoRelay.ChannelId.INPUT_2, ModbusType.UINT16) //
						.channel(10 + this.readOffset, SiemensLogoRelay.ChannelId.INPUT_3, ModbusType.UINT16) //
						.channel(11 + this.readOffset, SiemensLogoRelay.ChannelId.INPUT_4, ModbusType.UINT16) //

						.build()//
		);
	}

	@Override
	public String debugLog() {
		var outputLog = stream(this.digitalOutputChannels) //
				.map(c -> c.value().asOptional()) //
				.map(t -> t.isPresent() ? (t.get() ? "X" : "-") : "?") //
				.collect(joining(""));
		var inputLog = stream(this.digitalInputChannels) //
				.map(c -> c.value().asOptional()) //
				.map(t -> t.isPresent() ? (t.get() ? "I" : "O") : "?") //
				.collect(joining(""));
		return "Output:" + outputLog + "|Input:" + inputLog;
	}
}
