package io.openems.edge.meter.ddsu666;

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

import io.openems.edge.common.channel.Doc;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.common.modbusslave.ModbusType; 

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "MeterDDSU666", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterDDSU666Impl extends AbstractOpenemsModbusComponent implements MeterDDSU666, ElectricityMeter, ModbusSlave, ModbusComponent, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config = null;

	public MeterDDSU666Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				MeterDDSU666.ChannelId.values(), //
				ElectricityMeter.ChannelId.values()//
		);
		// Automatically calculate sum values from L1/L2/L3
				ElectricityMeter.calculateSumCurrentFromPhases(this);
				ElectricityMeter.calculateAverageVoltageFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if(super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id())) {
			return;
		}
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		// TODO implement ModbusProtocol
		return new ModbusProtocol(this,
				new FC3ReadRegistersTask(8192, Priority.HIGH,
						m(ElectricityMeter.ChannelId.VOLTAGE, new FloatDoublewordElement(8192))
						.wordOrder(WordOrder.MSWLSW),
						m(ElectricityMeter.ChannelId.CURRENT, new FloatDoublewordElement(8194))
						.wordOrder(WordOrder.MSWLSW),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(8196))
						.wordOrder(WordOrder.MSWLSW)
				),
				new FC3ReadRegistersTask(8202, Priority.HIGH,
						m(MeterDDSU666.ChannelId.POWER_FACTOR, new FloatDoublewordElement(8202))
						.wordOrder(WordOrder.MSWLSW),
						m(ElectricityMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(8206))
						.wordOrder(WordOrder.MSWLSW)
				),
				new FC3ReadRegistersTask(16384, Priority.LOW,
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new FloatDoublewordElement(16384))
	                	.wordOrder(WordOrder.MSWLSW)

	        	)
		);
	}

	private MeterType meterType = MeterType.PRODUCTION;
	@Override
	public MeterType getMeterType() { 
		return this.config.type();
	}
	
	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}
	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
	    return new ModbusSlaveTable(
	        // Các nature tables cơ bản
	        OpenemsComponent.getModbusSlaveNatureTable(accessMode),
	        ElectricityMeter.getModbusSlaveNatureTable(accessMode),
	        
	        // Custom channels của DDSu666
	        ModbusSlaveNatureTable.of(MeterDDSU666.class, accessMode, 100)
	            .channel(0, MeterDDSU666.ChannelId.POWER_FACTOR, ModbusType.FLOAT32)
	            .build()
	        );
	}
}
