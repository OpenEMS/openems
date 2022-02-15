package io.openems.edge.meter.algo2.emulator;

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
// import org.osgi.service.event.Event;
// import org.osgi.service.event.EventConstants;
// import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.Doc;
// import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
// import io.openems.edge.common.event.EdgeEventConstants;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "io.openems.edge.meter.algo2", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //, //
		// property = { //
		// 		EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		// } //
)
// public class EmulatorImpl extends AbstractOpenemsComponent 
//    implements Emulator, OpenemsComponent, EventHandler {
public class EmulatorImpl extends AbstractOpenemsModbusComponent
		implements Emulator, SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private MeterType meterType = MeterType.PRODUCTION;

	/*
	 * Invert power values
	 */
	private boolean invert = false;

	@Reference
	protected ConfigurationAdmin cm;

	// private Config config = null;

	public EmulatorImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();
		this.invert = config.invert();

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

//	@Override
//	public void handleEvent(Event event) {
//		if (!this.isEnabled()) {
//			return;
// 		}
//		switch (event.getTopic()) {
//		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
//			// TODO: fill channels
//			break;
//		}
//	}

	@Override
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		/*
		 * We are using the FLOAT registers from the modbus table, because they are all
		 * reachable within one ReadMultipleRegistersRequest.
		 */
		ModbusProtocol modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(0, Priority.HIGH, //
								 m(new SignedDoublewordElement(0)) //
								 	.m(AsymmetricMeter.ChannelId.VOLTAGE_L1, ElementToChannelConverter.DIRECT_1_TO_1) //
									// .m(SymmetricMeter.ChannelId.VOLTAGE, ElementToChannelConverter.SCALE_FACTOR_MINUS_3) //
									.build(), //
								// m(Emulator.ChannelId.VOLTAGE_L1, new SignedDoublewordElement(0),
								//		ElementToChannelConverter.SCALE_FACTOR_3),
								m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new SignedDoublewordElement(2),
										ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
								m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new SignedDoublewordElement(4),
										ElementToChannelConverter.SCALE_FACTOR_MINUS_3) // ,
//								m(AsymmetricMeter.ChannelId.VOLTAGE_L12, new SignedDoublewordElement(6),
//										ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
//								m(AsymmetricMeter.ChannelId.VOLTAGE_L23, new SignedDoublewordElement(8),
//										ElementToChannelConverter.SCALE_FACTOR_MINUS_3),
//								m(AsymmetricMeter.ChannelId.VOLTAGE_L31, new SignedDoublewordElement(0x0a),
//										ElementToChannelConverter.SCALE_FACTOR_MINUS_3) // ,
								// new DummyRegisterElement(0x, 0x1b) // , //
/*								m(AsymmetricMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(860),
										ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)),
								m(AsymmetricMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(862),
										ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)),
								m(AsymmetricMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(864),
										ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)),
								m(SymmetricMeter.ChannelId.CURRENT, new FloatDoublewordElement(866),
										ElementToChannelConverter.SCALE_FACTOR_3_AND_INVERT_IF_TRUE(this.invert)),
								m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(868),
										ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
								m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(870),
										ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
								m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(872),
										ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
								m(SymmetricMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(874),
										ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
								m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(876),
										ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
								m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(878),
										ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
								m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(880),
										ElementToChannelConverter.INVERT_IF_TRUE(this.invert)),
								m(SymmetricMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(882),
										ElementToChannelConverter.INVERT_IF_TRUE(this.invert))));*/
						
						
						
						
						
						
				)
		);

		if (this.invert) {
			modbusProtocol.addTask(
				new FC3ReadRegistersTask(0x1C, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new SignedDoublewordElement(0x1C))
				)
			);
		} else {
			modbusProtocol.addTask(
				new FC3ReadRegistersTask(0x1C, Priority.LOW, //
					m(SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new SignedDoublewordElement(0x1C))
				)
			);
		}

		return modbusProtocol;
	}

	@Override
	public String debugLog() {
		return "\n L:" + this.getActivePower().asString() 
				+ "\n - V1 - " + this.getVoltageL1().asString()
				+ "\n - V2 - " + this.getVoltageL2().asString()
				+ "\n - C - " + this.getActiveConsumptionEnergyChannel().toString()
				+ "\n - P - " + this.getActiveProductionEnergyChannel().toString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode) //
		);

	}
}
