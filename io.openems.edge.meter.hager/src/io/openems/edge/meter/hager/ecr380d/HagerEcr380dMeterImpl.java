package io.openems.edge.meter.hager.ecr380d;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
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
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
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

	protected HagerEcr380dMeterImpl() {
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
	 * Calculate a List of Registers, this modbus slave provides to the
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
				this.m(HagerEcr380dMeter.ChannelId.VENDOR_NAME), //
				this.m(HagerEcr380dMeter.ChannelId.PRODUCT_CODE), //
				this.m(HagerEcr380dMeter.ChannelId.SW_VERSION), //
				this.m(HagerEcr380dMeter.ChannelId.VENDOR_URL), //
				this.m(HagerEcr380dMeter.ChannelId.PRODUCT_NAME), //
				this.m(HagerEcr380dMeter.ChannelId.MODEL_NAME), //
				this.m(HagerEcr380dMeter.ChannelId.APPLICATION_NAME), //
				this.m(HagerEcr380dMeter.ChannelId.HW_VERSION), //
				this.m(HagerEcr380dMeter.ChannelId.PRODUCTION_CODE_SERIAL_NUMBER), //
				this.m(HagerEcr380dMeter.ChannelId.PRODUCTION_SITE_CODE), //
				this.m(HagerEcr380dMeter.ChannelId.PRODUCTION_DAY_OF_YEAR), //
				this.m(HagerEcr380dMeter.ChannelId.PRODUCTION_YEAR) //
		);
	}
	
	private FC3ReadRegistersTask getInstantaneousMeasuresTask() {
		return new FC3ReadRegistersTask(//
				INSTANTANEOUS_MEASURES_START_ADDRESS, //
				Priority.HIGH,
				this.m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement( 0x0000), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)),
				this.m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement( 0x0001), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)),
				this.m(ElectricityMeter.ChannelId.VOLTAGE_L1, new UnsignedWordElement( 0x0002), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)),
				this.m(HagerEcr380dMeter.ChannelId.V_L1_L2), //
				this.m(HagerEcr380dMeter.ChannelId.V_L2_L3), //
				this.m(HagerEcr380dMeter.ChannelId.V_L3_L1), //
				this.m(ElectricityMeter.ChannelId.FREQUENCY, new UnsignedWordElement(0x0006), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				new DummyRegisterElement(0x0007, 0x0008), //
				this.m(ElectricityMeter.ChannelId.CURRENT_L1, new UnsignedDoublewordElement(0x0009), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(ElectricityMeter.ChannelId.CURRENT_L2, new UnsignedDoublewordElement(0x000B), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(ElectricityMeter.ChannelId.CURRENT_L3, new UnsignedDoublewordElement(0x000D), ElementToChannelConverter.DIRECT_1_TO_1), //
				this.m(HagerEcr380dMeter.ChannelId.I_NEUTRAL), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(0x0011), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(0x0013), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.S_SUM), //
				this.m(HagerEcr380dMeter.ChannelId.PF_SUM_IEC), //
				this.m(HagerEcr380dMeter.ChannelId.PF_SUM_IEEE), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(0x0019), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(0x001B), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(0x001D), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new SignedDoublewordElement(0x001F), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new SignedDoublewordElement(0x0021), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new SignedDoublewordElement(0x0023), ElementToChannelScaleFactorConverter.MULTIPLY(10.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.S_L1), //
				this.m(HagerEcr380dMeter.ChannelId.S_L2), //
				this.m(HagerEcr380dMeter.ChannelId.S_L3), //
				this.m(HagerEcr380dMeter.ChannelId.PF_L1_IEC), //
				this.m(HagerEcr380dMeter.ChannelId.PF_L2_IEC), //
				this.m(HagerEcr380dMeter.ChannelId.PF_L3_IEC), //
				this.m(HagerEcr380dMeter.ChannelId.PF_L1_IEEE), //
				this.m(HagerEcr380dMeter.ChannelId.PF_L2_IEEE), //
				this.m(HagerEcr380dMeter.ChannelId.PF_L3_IEEE) //
		);
	}
	
	private FC3ReadRegistersTask getEnergyTask() {
		return new FC3ReadRegistersTask(//
				ENERGY_START_ADDRESS, //
				Priority.HIGH, //
				this.m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new UnsignedDoublewordElement(0x0000), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.ER_PLUS_SUM), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new UnsignedDoublewordElement(0x0004), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.ER_MINUS_SUM), //
				this.m(HagerEcr380dMeter.ChannelId.EA_PLUS_DETAILED_SUM), //
				this.m(HagerEcr380dMeter.ChannelId.EA_MINUS_DETAILED_SUM) //
		);
	}

	private FC3ReadRegistersTask getEnergyByPhaseTask() {
		return new FC3ReadRegistersTask(//
				ENERGY_PER_PHASE_START_ADDRESS, //
				Priority.HIGH, //
				this.m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L1, new UnsignedDoublewordElement(0x0000), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L2, new UnsignedDoublewordElement(0x0002), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY_L3, new UnsignedDoublewordElement(0x0004), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L1, new UnsignedDoublewordElement(0x0006), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L2, new UnsignedDoublewordElement(0x0008), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY_L3, new UnsignedDoublewordElement(0x000A), ElementToChannelScaleFactorConverter.MULTIPLY(1000.0d)), //
				this.m(HagerEcr380dMeter.ChannelId.ER_PLUS_L1),
				this.m(HagerEcr380dMeter.ChannelId.ER_PLUS_L2),
				this.m(HagerEcr380dMeter.ChannelId.ER_PLUS_L3),
				this.m(HagerEcr380dMeter.ChannelId.ER_MINUS_L1),
				this.m(HagerEcr380dMeter.ChannelId.ER_MINUS_L2),
				this.m(HagerEcr380dMeter.ChannelId.ER_MINUS_L3)
				
		);
	}

	/**
	 * Helper method to create a modbus register by a {@link HagerEcr380dMeter.ChannelId}.
	 * 
	 * @param channelId The channel description
	 * @return the element parameter
	 */
	private ModbusRegisterElement<?,?> m(HagerEcr380dMeter.ChannelId channelId) {
		return channelId.converter() != null ? 
				this.m(channelId, channelId.address(), channelId.converter()) //
				: this.m(channelId, channelId.address());
	}
}
