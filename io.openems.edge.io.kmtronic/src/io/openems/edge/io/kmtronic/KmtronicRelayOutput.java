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

import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.CoilElement;
import io.openems.edge.bridge.modbus.api.task.FC1ReadCoilsTask;
import io.openems.edge.bridge.modbus.api.task.FC5WriteCoilTask;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(name = "IO.KMtronic", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class KmtronicRelayOutput extends AbstractOpenemsModbusComponent implements DigitalOutput, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Holds writes to Relay Output 1 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_1(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Relay Output 1
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		RELAY_1(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_RELAY_1).setNextValue(value);
					});
				})),
		/**
		 * Holds writes to Relay Output 2 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_2(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Relay Output 2
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		RELAY_2(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_RELAY_2).setNextValue(value);
					});
				})),
		/**
		 * Holds writes to Relay Output 3 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_3(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Relay Output 3
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		RELAY_3(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_RELAY_3).setNextValue(value);
					});
				})),
		/**
		 * Holds writes to Relay Output 4 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_4(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Relay Output 4
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		RELAY_4(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_RELAY_4).setNextValue(value);
					});
				})),
		/**
		 * Holds writes to Relay Output 5 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_5(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Relay Output 5
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		RELAY_5(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_RELAY_5).setNextValue(value);
					});
				})),
		/**
		 * Holds writes to Relay Output 6 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_6(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Relay Output 6
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		RELAY_6(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_RELAY_6).setNextValue(value);
					});
				})),
		/**
		 * Holds writes to Relay Output 7 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_7(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Relay Output 7
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		RELAY_7(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_RELAY_7).setNextValue(value);
					});
				})),
		/**
		 * Holds writes to Relay Output 8 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_RELAY_8(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Relay Output 8
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		RELAY_8(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_RELAY_8).setNextValue(value);
					});
				}));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private final BooleanWriteChannel[] digitalOutputChannels;

	public KmtronicRelayOutput() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(KmtronicRelayOutput.ChannelId.RELAY_1), //
				this.channel(KmtronicRelayOutput.ChannelId.RELAY_2), //
				this.channel(KmtronicRelayOutput.ChannelId.RELAY_3), //
				this.channel(KmtronicRelayOutput.ChannelId.RELAY_4), //
				this.channel(KmtronicRelayOutput.ChannelId.RELAY_5), //
				this.channel(KmtronicRelayOutput.ChannelId.RELAY_6), //
				this.channel(KmtronicRelayOutput.ChannelId.RELAY_7), //
				this.channel(KmtronicRelayOutput.ChannelId.RELAY_8), //
		};
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				/*
				 * For Read: Read Coils
				 */
				new FC1ReadCoilsTask(0, Priority.LOW, //
						m(KmtronicRelayOutput.ChannelId.RELAY_1, new CoilElement(0)), //
						m(KmtronicRelayOutput.ChannelId.RELAY_2, new CoilElement(1)), //
						m(KmtronicRelayOutput.ChannelId.RELAY_3, new CoilElement(2)), //
						m(KmtronicRelayOutput.ChannelId.RELAY_4, new CoilElement(3)), //
						m(KmtronicRelayOutput.ChannelId.RELAY_5, new CoilElement(4)), //
						m(KmtronicRelayOutput.ChannelId.RELAY_6, new CoilElement(5)), //
						m(KmtronicRelayOutput.ChannelId.RELAY_7, new CoilElement(6)), //
						m(KmtronicRelayOutput.ChannelId.RELAY_8, new CoilElement(7)) //
				),
				/*
				 * For Write: Write Single Coil
				 */
				new FC5WriteCoilTask(0, m(KmtronicRelayOutput.ChannelId.RELAY_1, new CoilElement(0))), //
				new FC5WriteCoilTask(1, m(KmtronicRelayOutput.ChannelId.RELAY_2, new CoilElement(1))), //
				new FC5WriteCoilTask(2, m(KmtronicRelayOutput.ChannelId.RELAY_3, new CoilElement(2))), //
				new FC5WriteCoilTask(3, m(KmtronicRelayOutput.ChannelId.RELAY_4, new CoilElement(3))), //
				new FC5WriteCoilTask(4, m(KmtronicRelayOutput.ChannelId.RELAY_5, new CoilElement(4))), //
				new FC5WriteCoilTask(5, m(KmtronicRelayOutput.ChannelId.RELAY_6, new CoilElement(5))), //
				new FC5WriteCoilTask(6, m(KmtronicRelayOutput.ChannelId.RELAY_7, new CoilElement(6))), //
				new FC5WriteCoilTask(7, m(KmtronicRelayOutput.ChannelId.RELAY_8, new CoilElement(7))) //
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

}
