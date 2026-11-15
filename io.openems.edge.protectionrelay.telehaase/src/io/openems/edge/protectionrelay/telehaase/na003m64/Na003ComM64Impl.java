package io.openems.edge.protectionrelay.telehaase.na003m64;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "ProtectionRelay.Tele.Na003M64", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Modbus")
public class Na003ComM64Impl extends AbstractOpenemsModbusComponent
		implements Na003ComM64, ModbusComponent, OpenemsComponent {

	private Config config = null;

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))" //
	)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public Na003ComM64Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				Na003ComM64.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		super.activate(context, this.config.id(), this.config.alias(), this.config.enabled(),
				this.config.modbusUnitId());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(10000, Priority.LOW, //
						new BitsWordElement(10000, this) //
								.bit(0, Na003ComM64.ChannelId.LED_REL_1) //
								.bit(1, Na003ComM64.ChannelId.LED_REL_2) //
								.bit(2, Na003ComM64.ChannelId.LED_REL_3), //
						new BitsWordElement(10001, this) //
								.bit(0, Na003ComM64.ChannelId.DIGITAL_INPUT_1) //
								.bit(1, Na003ComM64.ChannelId.DIGITAL_INPUT_2) //
								.bit(2, Na003ComM64.ChannelId.DIGITAL_INPUT_3) //
								.bit(3, Na003ComM64.ChannelId.DIGITAL_INPUT_4) //
								.bit(4, Na003ComM64.ChannelId.DIGITAL_INPUT_5), //
						new DummyRegisterElement(10002, 10006), //
						new BitsWordElement(10007, this) //
								.bit(0, Na003ComM64.ChannelId.ERROR_LOGIC_OVERVOLTAGE_LL) //
								.bit(1, Na003ComM64.ChannelId.ERROR_LOGIC_UNDERVOLTAGE_LL) //
								.bit(2, Na003ComM64.ChannelId.ERROR_LOGIC_OVERVOLTAGE_LN) //
								.bit(3, Na003ComM64.ChannelId.ERROR_LOGIC_UNDERVOLTAGE_LN) //
								.bit(4, Na003ComM64.ChannelId.ERROR_LOGIC_TEN_MIN_OVERVOLTAGE) //
								.bit(5, Na003ComM64.ChannelId.ERROR_LOGIC_OVERFREQUENCY) //
								.bit(6, Na003ComM64.ChannelId.ERROR_LOGIC_UNDERFREQUENCY) //
								.bit(7, Na003ComM64.ChannelId.ERROR_LOGIC_FREQUENCY_CHANGE_RATE) //
								.bit(8, Na003ComM64.ChannelId.ERROR_LOGIC_PHASE_SHIFT) //
								.bit(9, Na003ComM64.ChannelId.ERROR_LOGIC_REMOTE_SHUTDOWN_SELF_TEST) //
								.bit(10, Na003ComM64.ChannelId.ERROR_LOGIC_ERROR_SYSTEM) //
								.bit(11, Na003ComM64.ChannelId.ERROR_LOGIC_CONTACT_REPORTS_CLOSED) //
								.bit(12, Na003ComM64.ChannelId.ERROR_LOGIC_CONTACT_REPORTS_OPEN) //
								.bit(13, Na003ComM64.ChannelId.ERROR_LOGIC_ERROR_DELAY_RUNNING) //
								.bit(14, Na003ComM64.ChannelId.ERROR_LOGIC_GOOD_DELAY_RUNNING) //
								.bit(15, Na003ComM64.ChannelId.ERROR_LOGIC_MASTER_ERROR), //
						m(Na003ComM64.ChannelId.GOOD_COUNTDOWN, new UnsignedWordElement(10008)), //
						new DummyRegisterElement(10009, 10010), //
						m(Na003ComM64.ChannelId.U_DELTA_1_2, new UnsignedWordElement(10011)), //
						m(Na003ComM64.ChannelId.U_DELTA_2_3, new UnsignedWordElement(10012)), //
						m(Na003ComM64.ChannelId.U_DELTA_3_1, new UnsignedWordElement(10013)), //
						m(Na003ComM64.ChannelId.U_STAR_1, new UnsignedWordElement(10014)), //
						m(Na003ComM64.ChannelId.U_STAR_2, new UnsignedWordElement(10015)), //
						m(Na003ComM64.ChannelId.U_STAR_3, new UnsignedWordElement(10016)), //
						m(Na003ComM64.ChannelId.U_AVG_1, new UnsignedWordElement(10017)), //
						m(Na003ComM64.ChannelId.U_AVG_2, new UnsignedWordElement(10018)), //
						m(Na003ComM64.ChannelId.U_AVG_3, new UnsignedWordElement(10019)), //
						m(Na003ComM64.ChannelId.FREQUENCY, new UnsignedWordElement(10020)) //
				), //
				new FC6WriteRegisterTask(50902, //
						m(Na003ComM64.ChannelId.MODBUS_ADDRESS, new UnsignedWordElement(50902)) //
				), //
				new FC6WriteRegisterTask(50903, //
						m(Na003ComM64.ChannelId.MODBUS_BAUDRATE, new UnsignedWordElement(50903)) //
				), //
				new FC6WriteRegisterTask(50904, //
						m(Na003ComM64.ChannelId.MODBUS_PROPERTIES, new UnsignedWordElement(50904)) //
				) //
		);
	}

	@Override
	public String debugLog() {
		var b = new StringBuilder();
		b.append("Frequency:")//
				.append(this.channel(Na003ComM64.ChannelId.FREQUENCY).value().asString());
		b.append("|Input:");

		var channels = this.getListOfDigitalChannels();
		for (var i = 0; i < channels.size(); i++) {
			b.append(channels.get(i).value().asOptional().map(v -> v //
					? "x"
					: "-") //
					.orElse("?"));
		}
		return b.toString();
	}

	private List<Channel<Boolean>> getListOfDigitalChannels() {
		return List.of(//
				this.channel(Na003ComM64.ChannelId.DIGITAL_INPUT_1),
				this.channel(Na003ComM64.ChannelId.DIGITAL_INPUT_2),
				this.channel(Na003ComM64.ChannelId.DIGITAL_INPUT_3),
				this.channel(Na003ComM64.ChannelId.DIGITAL_INPUT_4),
				this.channel(Na003ComM64.ChannelId.DIGITAL_INPUT_5));
	}
}
