package io.openems.edge.wago;

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

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(name = "WagoTest", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Wago extends AbstractOpenemsModbusComponent implements DigitalOutput, DigitalInput, OpenemsComponent {
	@Reference
	protected ConfigurationAdmin cm;
	private final BooleanWriteChannel[] digitalOutputChannels;
	private String modbusBridgeId;

	public Wago() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		this.digitalInputChannels = new BooleanReadChannel[] { //
				this.channel(Wago.ChannelId.INPUT_1), //
				this.channel(Wago.ChannelId.INPUT_2), //
				this.channel(Wago.ChannelId.INPUT_3), //
				this.channel(Wago.ChannelId.INPUT_4), //
				this.channel(Wago.ChannelId.INPUT_5), //
				this.channel(Wago.ChannelId.INPUT_6), //

		};

		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(Wago.ChannelId.OUTPUT_7), //
				this.channel(Wago.ChannelId.OUTPUT_8)//

		};
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
		this.modbusBridgeId = config.modbus_id();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		ModbusProtocol protocol = new ModbusProtocol(unitId, //
				new FC1ReadCoilsTask(0, Priority.HIGH, //
						m(Wago.ChannelId.INPUT_1, new CoilElement(0)), //
						m(Wago.ChannelId.INPUT_2, new CoilElement(0)), //
						m(Wago.ChannelId.INPUT_3, new CoilElement(0)), //
						m(Wago.ChannelId.INPUT_4, new CoilElement(0)), //
						m(Wago.ChannelId.INPUT_5, new CoilElement(0)), //
						m(Wago.ChannelId.INPUT_6, new CoilElement(0))), //
				new FC1ReadCoilsTask(512, Priority.HIGH, //
						m(Wago.ChannelId.OUTPUT_7, new CoilElement(512)), //
						m(Wago.ChannelId.OUTPUT_8, new CoilElement(513))), //

				new FC5WriteCoilTask(512, m(Wago.ChannelId.OUTPUT_7, new CoilElement(512))), //
				new FC5WriteCoilTask(513, m(Wago.ChannelId.OUTPUT_8, new CoilElement(513)))//
		);
		return protocol;
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {

		INPUT_1(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		INPUT_2(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		INPUT_3(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		INPUT_4(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		INPUT_5(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		INPUT_6(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		OUTPUT_7(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		OUTPUT_8(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)) //

		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public String getModbusBridgeId() {
		return modbusBridgeId;
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
			if (++i <= this.digitalOutputChannels.length) {
				b.append(" ");
			}
		}
		return b.toString();
	}

	@Override
	public Channel<Boolean>[] digitalInputChannels() {
		return this.digitalInputChannels();
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}
	
}
