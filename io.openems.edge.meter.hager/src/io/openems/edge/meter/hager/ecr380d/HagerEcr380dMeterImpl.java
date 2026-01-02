package io.openems.edge.meter.hager.ecr380d;

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

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ElementToChannelScaleFactorConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.modbusslave.ModbusType;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;

/**
 * <a href="https://hager.com/de/katalog/produkt/ecr380d-energiezaehler-3ph-direkt-80a-modbus-mid">Hager ECR380D</a>
 * energy meter via Modbus RTU.
 *
 * @see HagerEcr380dMeter for detailed mapping information
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Hager.ECR380D", //
		immediate = true, // 
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class HagerEcr380dMeterImpl extends AbstractOpenemsModbusComponent //
	implements HagerEcr380dMeter, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config;

	public HagerEcr380dMeterImpl() {
		super(OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				HagerEcr380dMeter.ChannelId.values()//
		);
	}

	@Activate
	@Modified
	protected void activate(final ComponentContext context, final Config config) throws OpenemsException {
		this.config = config;
		
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus", config.modbus_id());
	}


	@Deactivate
	@Override
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public String debugLog() {
		final Value<Integer> p = this.getActivePower();
		return "P=" + (p.isDefined() ? p.get() + " W" : "-");
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	/**
	 * Calculate a List of registers, this modbus slave provides to the
	 * outside world.
	 * 
	 * @param accessMode the filter which registers are available
	 * @return the table of registers, this implementation present to clients
	 */
	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(HagerEcr380dMeter.class, accessMode, 0x002C) //
					.channel(0x0000, HagerEcr380dMeter.ChannelId.V_L1_L2, ModbusType.UINT32) //
					.channel(0x0002, HagerEcr380dMeter.ChannelId.V_L2_L3, ModbusType.UINT32) //
					.channel(0x0004, HagerEcr380dMeter.ChannelId.V_L3_L1, ModbusType.UINT32) //
					.channel(0x0006, HagerEcr380dMeter.ChannelId.ER_PLUS_L1, ModbusType.UINT32) //
					.channel(0x0008, HagerEcr380dMeter.ChannelId.I_NEUTRAL, ModbusType.UINT64) //
					.channel(0x000C, HagerEcr380dMeter.ChannelId.ER_PLUS_SUM, ModbusType.UINT64) //
					.channel(0x0010, HagerEcr380dMeter.ChannelId.ER_MINUS_SUM, ModbusType.UINT64) //
					.channel(0x0014, HagerEcr380dMeter.ChannelId.ER_PLUS_L1, ModbusType.UINT64) //
					.channel(0x0018, HagerEcr380dMeter.ChannelId.ER_PLUS_L2, ModbusType.UINT64) //
					.channel(0x001C, HagerEcr380dMeter.ChannelId.ER_PLUS_L3, ModbusType.UINT64) //
					.channel(0x0020, HagerEcr380dMeter.ChannelId.ER_MINUS_L1, ModbusType.UINT64) //
					.channel(0x0024, HagerEcr380dMeter.ChannelId.ER_MINUS_L2, ModbusType.UINT64) //
					.channel(0x0028, HagerEcr380dMeter.ChannelId.ER_MINUS_L3, ModbusType.UINT64) //
					.build()
				);
	}

	/**
	 * Calculate the read commands used to read values from the device.
	 * 
	 * @return the modbus protocol to requests value from the device
	 */
	@Override
	protected ModbusProtocol defineModbusProtocol() {
		final ModbusProtocol modbusProtocol = new ModbusProtocol(this);
		
		modbusProtocol.addTask(this.getInstantaneousMeasuresTask());
				
		modbusProtocol.addTask(this.getEnergyTask());
		
		modbusProtocol.addTask(this.getEnergyByPhaseTask());
		
		modbusProtocol.addTask(this.getDeviceTask());

		return modbusProtocol;
	}

	private FC3ReadRegistersTask getDeviceTask() {
		return new FC3ReadRegistersTask(//
				DEVICE_START_ADDRESS, //
				Priority.LOW,
				this.m(HagerEcr380dMeter.ChannelId.VENDOR_NAME, new StringWordElement(DEVICE_START_ADDRESS | 0x0000, 16), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(HagerEcr380dMeter.ChannelId.PRODUCT_CODE, new StringWordElement(DEVICE_START_ADDRESS | 0x0010, 16), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(HagerEcr380dMeter.ChannelId.SW_VERSION, new StringWordElement(DEVICE_START_ADDRESS | 0x0020, 2), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(HagerEcr380dMeter.ChannelId.VENDOR_URL, new StringWordElement(DEVICE_START_ADDRESS | 0x0022, 16), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(HagerEcr380dMeter.ChannelId.PRODUCT_NAME, new StringWordElement(DEVICE_START_ADDRESS | 0x0032, 16), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(HagerEcr380dMeter.ChannelId.MODEL_NAME, new StringWordElement(DEVICE_START_ADDRESS | 0x0042, 16), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(HagerEcr380dMeter.ChannelId.APPLICATION_NAME, new StringWordElement(DEVICE_START_ADDRESS | 0x0052, 16), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(HagerEcr380dMeter.ChannelId.HW_VERSION, new StringWordElement(DEVICE_START_ADDRESS | 0x0062, 2), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(HagerEcr380dMeter.ChannelId.PRODUCTION_CODE_SERIAL_NUMBER, new StringWordElement(DEVICE_START_ADDRESS | 0x0064, 16), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(HagerEcr380dMeter.ChannelId.PRODUCTION_SITE_CODE, new StringWordElement(DEVICE_START_ADDRESS | 0x0074, 2), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(HagerEcr380dMeter.ChannelId.PRODUCTION_DAY_OF_YEAR, new UnsignedWordElement(DEVICE_START_ADDRESS | 0x0076), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(HagerEcr380dMeter.ChannelId.PRODUCTION_YEAR, new UnsignedWordElement(DEVICE_START_ADDRESS | 0x0077), ElementToChannelConverter.DIRECT_1_TO_1) //
		);
	}
	
	private FC3ReadRegistersTask getInstantaneousMeasuresTask() {
		return new FC3ReadRegistersTask(//
				INSTANTANEOUS_MEASURES_START_ADDRESS, //
				Priority.HIGH,
				this.m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0000), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)),
				this.m(ElectricityMeter.ChannelId.VOLTAGE_L2, new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0001), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)),
				this.m(ElectricityMeter.ChannelId.VOLTAGE_L3, new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0002), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)),
				this.m(HagerEcr380dMeter.ChannelId.V_L1_L2, new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0003), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.V_L2_L3, new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0004), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.V_L3_L1, new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0005), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0006), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				new DummyRegisterElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0007, INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0008), //
				this.m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0009), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x000B), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x000D), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(HagerEcr380dMeter.ChannelId.I_NEUTRAL, new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x000F), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0011), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0013), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.S_SUM, new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0015), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.PF_SUM_IEC, new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0017), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(HagerEcr380dMeter.ChannelId.PF_SUM_IEEE, new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0018), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0019), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x001B), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x001D), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x001F), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0021), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0023), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.S_L1, new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0025), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.S_L2, new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0027), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.S_L3, new UnsignedDoublewordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0029), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.PF_L1_IEC, new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x002B), ElementToChannelScaleFactorConverter.DIVIDE(1000.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.PF_L2_IEC, new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x002C), ElementToChannelScaleFactorConverter.DIVIDE(1000.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.PF_L3_IEC, new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x002D), ElementToChannelScaleFactorConverter.DIVIDE(1000.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.PF_L1_IEEE, new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x002E), ElementToChannelScaleFactorConverter.DIVIDE(1000.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.PF_L2_IEEE, new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x002F), ElementToChannelScaleFactorConverter.DIVIDE(1000.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.PF_L3_IEEE, new SignedWordElement(INSTANTANEOUS_MEASURES_START_ADDRESS | 0x0030), ElementToChannelScaleFactorConverter.DIVIDE(1000.0d)) //
		);
	}
	
	private FC3ReadRegistersTask getEnergyTask() {
		return new FC3ReadRegistersTask(//
				ENERGY_START_ADDRESS, //
				Priority.HIGH, //
				this.m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(ENERGY_START_ADDRESS | 0x0000), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.ER_PLUS_SUM, new UnsignedDoublewordElement(ENERGY_START_ADDRESS | 0x0002), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(ENERGY_START_ADDRESS | 0x0004), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.ER_MINUS_SUM, new UnsignedDoublewordElement(ENERGY_START_ADDRESS | 0x0006), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.EA_PLUS_DETAILED_SUM, new UnsignedDoublewordElement(ENERGY_START_ADDRESS | 0x0008), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.EA_MINUS_DETAILED_SUM, new UnsignedDoublewordElement(ENERGY_START_ADDRESS | 0x000A), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)) //
		);
	}

	private FC3ReadRegistersTask getEnergyByPhaseTask() {
		return new FC3ReadRegistersTask(//
				ENERGY_PER_PHASE_START_ADDRESS, //
				Priority.HIGH, //
				this.m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0000), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0002), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0004), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0006), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0008), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x000A), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.ER_PLUS_L1, new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x000C), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
				this.m(HagerEcr380dMeter.ChannelId.ER_PLUS_L2, new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x000E), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
				this.m(HagerEcr380dMeter.ChannelId.ER_PLUS_L3, new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0010), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
				this.m(HagerEcr380dMeter.ChannelId.ER_MINUS_L1, new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0012), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
				this.m(HagerEcr380dMeter.ChannelId.ER_MINUS_L2, new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0014), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)),
				this.m(HagerEcr380dMeter.ChannelId.ER_MINUS_L3, new UnsignedDoublewordElement(ENERGY_PER_PHASE_START_ADDRESS | 0x0016), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d))
				
		);
	}

}
