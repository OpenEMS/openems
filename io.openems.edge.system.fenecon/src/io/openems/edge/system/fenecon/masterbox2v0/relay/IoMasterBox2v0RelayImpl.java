package io.openems.edge.system.fenecon.masterbox2v0.relay;

import java.util.stream.Stream;

import io.openems.common.channel.AccessMode;
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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ChannelMetaInfoReadAndWrite;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Fenecon.MasterBox2V0.Relay", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class IoMasterBox2v0RelayImpl extends AbstractOpenemsModbusComponent
		implements IoMasterBox2v0Relay, DigitalOutput, DigitalInput, ModbusComponent, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private BooleanWriteChannel[] digitalOutputChannels;
	private BooleanReadChannel[] digitalInputChannels;

	public IoMasterBox2v0RelayImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				IoMasterBox2v0Relay.ChannelId.values() //
		);

		this.initializeChannels();
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {

		return new ModbusProtocol(this, //

				new FC3ReadRegistersTask(105, Priority.LOW, //
						m(IoMasterBox2v0Relay.ChannelId.RELAY_1, new UnsignedWordElement(105),
								new ChannelMetaInfoReadAndWrite(105, 1)), //
						m(IoMasterBox2v0Relay.ChannelId.RELAY_2, new UnsignedWordElement(106),
								new ChannelMetaInfoReadAndWrite(106, 2)), //
						m(IoMasterBox2v0Relay.ChannelId.RELAY_3, new UnsignedWordElement(107),
								new ChannelMetaInfoReadAndWrite(107, 3)), //
						m(IoMasterBox2v0Relay.ChannelId.RELAY_4, new UnsignedWordElement(108),
								new ChannelMetaInfoReadAndWrite(108, 4)), //
						m(IoMasterBox2v0Relay.ChannelId.RELAY_5, new UnsignedWordElement(109),
								new ChannelMetaInfoReadAndWrite(109, 5)), //
						m(IoMasterBox2v0Relay.ChannelId.RELAY_6, new UnsignedWordElement(110),
								new ChannelMetaInfoReadAndWrite(110, 6))),

				new FC6WriteRegisterTask(1, //
						m(IoMasterBox2v0Relay.ChannelId.RELAY_1, new UnsignedWordElement(1),
								new ChannelMetaInfoReadAndWrite(105, 1))), //
				new FC6WriteRegisterTask(2, //
						m(IoMasterBox2v0Relay.ChannelId.RELAY_2, new UnsignedWordElement(2),
								new ChannelMetaInfoReadAndWrite(106, 2))), //
				new FC6WriteRegisterTask(3, //
						m(IoMasterBox2v0Relay.ChannelId.RELAY_3, new UnsignedWordElement(3),
								new ChannelMetaInfoReadAndWrite(107, 3))), //
				new FC6WriteRegisterTask(4, //
						m(IoMasterBox2v0Relay.ChannelId.RELAY_4, new UnsignedWordElement(4),
								new ChannelMetaInfoReadAndWrite(108, 4))), //
				new FC6WriteRegisterTask(5, //
						m(IoMasterBox2v0Relay.ChannelId.RELAY_5, new UnsignedWordElement(5),
								new ChannelMetaInfoReadAndWrite(109, 5))), //
				new FC6WriteRegisterTask(6, //
						m(IoMasterBox2v0Relay.ChannelId.RELAY_6, new UnsignedWordElement(6),
								new ChannelMetaInfoReadAndWrite(110, 6))) //
		);
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
	public String debugLog() {
		var b = new StringBuilder();
		var i = 1;
		for (BooleanReadChannel channel : this.digitalInputChannels) {
			String valueText;
			var valueOpt = channel.value().asOptional();
			valueText = valueOpt.map(aBoolean -> aBoolean ? "x" : "-").orElse("?");
			b.append(i).append(valueText);

			// add space for all but the last
			if (++i <= this.digitalInputChannels.length) {
				b.append(" ");
			}
		}
		return b.toString();
	}

	private void initializeChannels() {
		this.digitalOutputChannels = Stream.of(IoMasterBox2v0Relay.ChannelId.values()) //
				.filter(channelId -> channelId.doc().getAccessMode() == AccessMode.READ_WRITE) //
				.map(this::channel) //
				.toArray(BooleanWriteChannel[]::new);

		this.digitalInputChannels = Stream.of(IoMasterBox2v0Relay.ChannelId.values()) //
				.filter(channelId -> channelId.doc().getAccessMode() == AccessMode.READ_ONLY) //
				.map(this::channel) //
				.toArray(BooleanReadChannel[]::new);
	}
}
