package io.openems.edge.system.fenecon.masterbox2v0;

import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ChannelMetaInfoReadAndWrite;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.BitsWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IOC.Fenecon.MasterBox2V0", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Modbus")
public class MasterBox2v0Impl extends AbstractOpenemsModbusComponent
		implements MasterBox2v0, ModbusComponent, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(//
			policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public MasterBox2v0Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				MasterBox2v0.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
	}

	@Modified
	protected void modified(ComponentContext context, Config config) throws OpenemsException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
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
				new FC3ReadRegistersTask(8, Priority.LOW, //
						m(MasterBox2v0.ChannelId.ANALOG_OUT_VOLTAGE, new UnsignedWordElement(8),
								ElementToChannelConverter.SCALE_FACTOR_2), //
						m(MasterBox2v0.ChannelId.ANALOG_OUT_CONTROL, new UnsignedWordElement(9))), //

				new FC3ReadRegistersTask(101, Priority.LOW, //
						m(new BitsWordElement(101, this) //
								.bit(0, MasterBox2v0.ChannelId.HW_STARTUP) //
								.bit(1, MasterBox2v0.ChannelId.HW_TEMPERATURE_SENSOR_TYPE) //
								.bit(2, MasterBox2v0.ChannelId.HW_ANALOG_OUT_MODE) //
								.bit(3, MasterBox2v0.ChannelId.HW_SPI_ENERGY_ENABLE) //
								.bit(4, MasterBox2v0.ChannelId.HW_CAN_TOWER_ENABLE)), //
						m(MasterBox2v0.ChannelId.GRID_STATE, new UnsignedWordElement(102)), //
						m(MasterBox2v0.ChannelId.TEMPERATURE, new UnsignedWordElement(103), //
								ElementToChannelConverter.chain(ElementToChannelConverter.SCALE_FACTOR_MINUS_1,
										ElementToChannelConverter.SUBTRACT(40))), //
						m(MasterBox2v0.ChannelId.HUMIDITY, new UnsignedWordElement(104), //
								ElementToChannelConverter.SCALE_FACTOR_MINUS_1)), //

				new FC3ReadRegistersTask(105, Priority.HIGH, //
						m(MasterBox2v0.ChannelId.RELAY_1, new UnsignedWordElement(105),
								new ChannelMetaInfoReadAndWrite(105, 1)), //
						m(MasterBox2v0.ChannelId.RELAY_2, new UnsignedWordElement(106),
								new ChannelMetaInfoReadAndWrite(106, 2)), //
						m(MasterBox2v0.ChannelId.RELAY_3, new UnsignedWordElement(107),
								new ChannelMetaInfoReadAndWrite(107, 3)), //
						m(MasterBox2v0.ChannelId.RELAY_4, new UnsignedWordElement(108),
								new ChannelMetaInfoReadAndWrite(108, 4)), //
						m(MasterBox2v0.ChannelId.RELAY_5, new UnsignedWordElement(109),
								new ChannelMetaInfoReadAndWrite(109, 5)), //
						m(MasterBox2v0.ChannelId.RELAY_6, new UnsignedWordElement(110),
								new ChannelMetaInfoReadAndWrite(110, 6))),

				new FC3ReadRegistersTask(301, Priority.HIGH, //
						m(MasterBox2v0.ChannelId.VOLTAGE_L1_ENERGYMETER, new UnsignedWordElement(301), //
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(MasterBox2v0.ChannelId.VOLTAGE_L2_ENERGYMETER, new UnsignedWordElement(302), //
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(MasterBox2v0.ChannelId.VOLTAGE_L3_ENERGYMETER, new UnsignedWordElement(303), //
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(MasterBox2v0.ChannelId.CURRENT_L1_ENERGYMETER, new UnsignedWordElement(304), //
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(MasterBox2v0.ChannelId.CURRENT_L2_ENERGYMETER, new UnsignedWordElement(305), //
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(MasterBox2v0.ChannelId.CURRENT_L3_ENERGYMETER, new UnsignedWordElement(306), //
								ElementToChannelConverter.SCALE_FACTOR_1), //
						m(MasterBox2v0.ChannelId.ACTIVE_POWER_L1_ENERGYMETER, new UnsignedWordElement(307)), //
						m(MasterBox2v0.ChannelId.ACTIVE_POWER_L2_ENERGYMETER, new UnsignedWordElement(308)), //
						m(MasterBox2v0.ChannelId.ACTIVE_POWER_L3_ENERGYMETER, new UnsignedWordElement(309))),

				new FC3ReadRegistersTask(313, Priority.LOW, //
						m(MasterBox2v0.ChannelId.TIME_STAMP_ENERGYMETER, new UnsignedDoublewordElement(313)), //
						m(MasterBox2v0.ChannelId.STATUS_ENERGYMETER, new UnsignedWordElement(315))),

				new FC6WriteRegisterTask(1, //
						m(MasterBox2v0.ChannelId.RELAY_1, new UnsignedWordElement(1),
								new ChannelMetaInfoReadAndWrite(105, 1))), //
				new FC6WriteRegisterTask(2, //
						m(MasterBox2v0.ChannelId.RELAY_2, new UnsignedWordElement(2),
								new ChannelMetaInfoReadAndWrite(106, 2))), //
				new FC6WriteRegisterTask(3, //
						m(MasterBox2v0.ChannelId.RELAY_3, new UnsignedWordElement(3),
								new ChannelMetaInfoReadAndWrite(107, 3))), //
				new FC6WriteRegisterTask(4, //
						m(MasterBox2v0.ChannelId.RELAY_4, new UnsignedWordElement(4),
								new ChannelMetaInfoReadAndWrite(108, 4))), //
				new FC6WriteRegisterTask(5, //
						m(MasterBox2v0.ChannelId.RELAY_5, new UnsignedWordElement(5),
								new ChannelMetaInfoReadAndWrite(109, 5))), //
				new FC6WriteRegisterTask(6, //
						m(MasterBox2v0.ChannelId.RELAY_6, new UnsignedWordElement(6),
								new ChannelMetaInfoReadAndWrite(110, 6))), //
				new FC6WriteRegisterTask(8, //
						m(MasterBox2v0.ChannelId.ANALOG_OUT_VOLTAGE, new UnsignedWordElement(8),
								ElementToChannelConverter.SCALE_FACTOR_2)), //
				new FC6WriteRegisterTask(9, //
						m(MasterBox2v0.ChannelId.ANALOG_OUT_CONTROL, new UnsignedWordElement(9))));
	}
}
