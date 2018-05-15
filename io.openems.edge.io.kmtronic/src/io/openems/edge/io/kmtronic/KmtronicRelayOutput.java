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
import io.openems.edge.bridge.modbus.api.task.Priority;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.DigitalOutput;

@Designate(ocd = Config.class, factory = true)
@Component(name = "IO.KMtronic", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class KmtronicRelayOutput extends AbstractOpenemsModbusComponent implements DigitalOutput, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/**
		 * Holds writes to Digital Output 1 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_DIGITAL_OUTPUT_1(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Digital Output 1
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		DIGITAL_OUTPUT_1(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_DIGITAL_OUTPUT_1).setNextValue(value);
					});
				})),
		/**
		 * Holds writes to Digital Output 2 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_DIGITAL_OUTPUT_2(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Digital Output 2
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		DIGITAL_OUTPUT_2(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_DIGITAL_OUTPUT_2).setNextValue(value);
					});
				})),
		/**
		 * Holds writes to Digital Output 3 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_DIGITAL_OUTPUT_3(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Digital Output 3
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		DIGITAL_OUTPUT_3(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_DIGITAL_OUTPUT_2).setNextValue(value);
					});
				})),
		/**
		 * Holds writes to Digital Output 4 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_DIGITAL_OUTPUT_4(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Digital Output 4
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		DIGITAL_OUTPUT_4(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_DIGITAL_OUTPUT_4).setNextValue(value);
					});
				})),
		/**
		 * Holds writes to Digital Output 5 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_DIGITAL_OUTPUT_5(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Digital Output 5
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		DIGITAL_OUTPUT_5(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_DIGITAL_OUTPUT_5).setNextValue(value);
					});
				})),
		/**
		 * Holds writes to Digital Output 6 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_DIGITAL_OUTPUT_6(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Digital Output 6
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		DIGITAL_OUTPUT_6(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_DIGITAL_OUTPUT_6).setNextValue(value);
					});
				})),
		/**
		 * Holds writes to Digital Output 7 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_DIGITAL_OUTPUT_7(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Digital Output 7
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		DIGITAL_OUTPUT_7(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_DIGITAL_OUTPUT_7).setNextValue(value);
					});
				})),
		/**
		 * Holds writes to Digital Output 8 for debugging
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		DEBUG_DIGITAL_OUTPUT_8(new Doc().type(OpenemsType.BOOLEAN).unit(Unit.ON_OFF)), //
		/**
		 * Digital Output 8
		 * 
		 * <ul>
		 * <li>Interface: KmtronicRelayOutput
		 * <li>Type: Boolean
		 * <li>Range: On/Off
		 * </ul>
		 */
		@SuppressWarnings("unchecked")
		DIGITAL_OUTPUT_8(new Doc() //
				.type(OpenemsType.BOOLEAN) //
				.unit(Unit.ON_OFF) //
				.onInit(channel -> { //
					// on each setNextWrite to the channel -> store the value in the DEBUG-channel
					((WriteChannel<Boolean>) channel).onSetNextWrite(value -> {
						channel.getComponent().channel(ChannelId.DEBUG_DIGITAL_OUTPUT_8).setNextValue(value);
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
		this.digitalOutputChannels = new BooleanWriteChannel[] {
				this.channel(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_1), //
				this.channel(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_2), //
				this.channel(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_3), //
				this.channel(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_4), //
				this.channel(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_5), //
				this.channel(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_6), //
				this.channel(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_7), //
				this.channel(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_8), //
		};
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol(int unitId) {
		return new ModbusProtocol(unitId, //
				/*
				 * For Read: Read Coils
				 */
				new FC1ReadCoilsTask(0, Priority.LOW, //
						m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_1, new CoilElement(0)), //
						m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_2, new CoilElement(1)), //
						m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_3, new CoilElement(2)), //
						m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_4, new CoilElement(3)), //
						m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_5, new CoilElement(4)), //
						m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_6, new CoilElement(5)), //
						m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_7, new CoilElement(6)), //
						m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_8, new CoilElement(7)) //
				),
				/*
				 * For Write: Write Single Coil
				 */
				new FC5WriteCoilTask(0, m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_1, new CoilElement(0))), //
				new FC5WriteCoilTask(1, m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_2, new CoilElement(1))), //
				new FC5WriteCoilTask(2, m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_3, new CoilElement(2))), //
				new FC5WriteCoilTask(3, m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_4, new CoilElement(3))), //
				new FC5WriteCoilTask(4, m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_5, new CoilElement(4))), //
				new FC5WriteCoilTask(5, m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_6, new CoilElement(5))), //
				new FC5WriteCoilTask(6, m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_7, new CoilElement(6))), //
				new FC5WriteCoilTask(7, m(KmtronicRelayOutput.ChannelId.DIGITAL_OUTPUT_8, new CoilElement(7))) //
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
