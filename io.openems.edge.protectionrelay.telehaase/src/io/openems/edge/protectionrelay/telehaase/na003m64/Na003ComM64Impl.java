package io.openems.edge.protectionrelay.telehaase.na003m64;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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
				new FC3ReadRegistersTask(10000, Priority.HIGH, //
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
				new FC3ReadRegistersTask(30010, Priority.LOW, //
						m(Na003ComM64.ChannelId.OVERVOLTAGE_1_LINE_TO_LINE_SEL, new UnsignedWordElement(30010)), //
						new DummyRegisterElement(30011, 30013), //
						m(Na003ComM64.ChannelId.UNDERVOLTAGE_1_LINE_TO_LINE_SEL, new UnsignedWordElement(30014)), //
						new DummyRegisterElement(30015, 30017), //
						m(Na003ComM64.ChannelId.OVERVOLTAGE_1_LINE_TO_NEUTRAL_SEL, new UnsignedWordElement(30018)), //
						new DummyRegisterElement(30019, 30021), //
						m(Na003ComM64.ChannelId.UNDERVOLTAGE_1_LINE_TO_NEUTRAL_SEL, new UnsignedWordElement(30022)), //
						new DummyRegisterElement(30023, 30025), //
						m(Na003ComM64.ChannelId.OVERVOLTAGE_2_LINE_TO_LINE_SEL, new UnsignedWordElement(30026)), //
						new DummyRegisterElement(30027, 30029), //
						m(Na003ComM64.ChannelId.UNDERVOLTAGE_2_LINE_TO_LINE_SEL, new UnsignedWordElement(30030)), //
						new DummyRegisterElement(30031, 30033), //
						m(Na003ComM64.ChannelId.OVERVOLTAGE_2_LINE_TO_NEUTRAL_SEL, new UnsignedWordElement(30034)), //
						new DummyRegisterElement(30035, 30037), //
						m(Na003ComM64.ChannelId.UNDERVOLTAGE_2_LINE_TO_NEUTRAL_SEL, new UnsignedWordElement(30038)), //
						new DummyRegisterElement(30039, 30041), //
						m(Na003ComM64.ChannelId.OVERVOLTAGE_10_MIN_AVG_SEL, new UnsignedWordElement(30042)), //
						new DummyRegisterElement(30043, 30053), //
						m(Na003ComM64.ChannelId.OVERFREQUENCY_1_SEL, new UnsignedWordElement(30054)), //
						new DummyRegisterElement(30055, 30057), //
						m(Na003ComM64.ChannelId.UNDERFREQUENCY_1_SEL, new UnsignedWordElement(30058)), //
						new DummyRegisterElement(30059, 30061), //
						m(Na003ComM64.ChannelId.OVERFREQUENCY_2_SEL, new UnsignedWordElement(30062)), //
						new DummyRegisterElement(30063, 30065), //
						m(Na003ComM64.ChannelId.UNDERFREQUENCY_2_SEL, new UnsignedWordElement(30066)), //
						new DummyRegisterElement(30067, 30069), //
						m(Na003ComM64.ChannelId.OVERFREQUENCY_3_SEL, new UnsignedWordElement(30070)), //
						new DummyRegisterElement(30071, 30073), //
						m(Na003ComM64.ChannelId.UNDERFREQUENCY_3_SEL, new UnsignedWordElement(30074)), //
						new DummyRegisterElement(30075, 30085), //
						m(Na003ComM64.ChannelId.FREQUENCY_RANDOM_SEL, new UnsignedWordElement(30086)), //
						new DummyRegisterElement(30087, 30089), //
						m(Na003ComM64.ChannelId.FREQUENCY_CHANGE_RATE_SEL, new UnsignedWordElement(30090)), //
						new DummyRegisterElement(30091, 30093), //
						m(Na003ComM64.ChannelId.PHASE_SHIFT_SEL, new UnsignedWordElement(30094)), //
						new DummyRegisterElement(30095, 30098), //
						m(Na003ComM64.ChannelId.CONTACT, new UnsignedWordElement(30099)), //
						m(Na003ComM64.ChannelId.CONTACT_DELAY, new UnsignedWordElement(30100)), //
						new DummyRegisterElement(30101), //
						m(Na003ComM64.ChannelId.ON_DELAY, new UnsignedWordElement(30102)), //
						m(Na003ComM64.ChannelId.ON_DELAY_R_SEL, new UnsignedWordElement(30103)), //
						m(Na003ComM64.ChannelId.ON_DELAY_R, new UnsignedWordElement(30104)), //
						new DummyRegisterElement(30105, 30113), //
						m(Na003ComM64.ChannelId.DIGITAL_INPUT_3_STATE, new UnsignedWordElement(30114)), //
						m(Na003ComM64.ChannelId.U_ZERO_O_SEL, new UnsignedWordElement(30115)), //
						new DummyRegisterElement(30116, 30118), //
						m(Na003ComM64.ChannelId.CEN_URES_O_SEL, new UnsignedWordElement(30119)), //
						new DummyRegisterElement(30120, 30122), //
						m(Na003ComM64.ChannelId.CEN_LN_U_SEL, new UnsignedWordElement(30123)) //
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
}
