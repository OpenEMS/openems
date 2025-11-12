package io.openems.edge.meter.pro380modct;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.api.*;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.ElectricityMeter;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "io.openems.edge.meter.pro380modct", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class Pro380modctImpl extends AbstractOpenemsModbusComponent implements Pro380modct, ElectricityMeter, ModbusComponent, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config = null;

	public Pro380modctImpl() {
        super(//
                OpenemsComponent.ChannelId.values(), //
                ModbusComponent.ChannelId.values(), //
                ElectricityMeter.ChannelId.values(), //
                Pro380modct.ChannelId.values() //
        );
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
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
        return new ModbusProtocol(
                this,

                // VOLTAGE + FREQUENCY
                new FC3ReadRegistersTask(
                        0x5002, Priority.HIGH,
                        //m(Pro380modct.ChannelId.VOLTAGE,    new FloatDoublewordElement(0x5000).wordOrder(WordOrder.MSWLSW)),
                        m(ElectricityMeter.ChannelId.VOLTAGE_L1, new FloatDoublewordElement(0x5002).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(ElectricityMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(0x5004).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(ElectricityMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(0x5006).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(ElectricityMeter.ChannelId.FREQUENCY,  new FloatDoublewordElement(0x5008).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3)
                ),

                // CURRENT (total + L1/L2/L3)
                new FC3ReadRegistersTask(
                        0x500A, Priority.HIGH,
                        m(ElectricityMeter.ChannelId.CURRENT,    new FloatDoublewordElement(0x500A).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(ElectricityMeter.ChannelId.CURRENT_L1, new FloatDoublewordElement(0x500C).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(ElectricityMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(0x500E).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(ElectricityMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(0x5010).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3)
                ),

                // ACTIVE POWER (kW) total + L1/L2/L3
                new FC3ReadRegistersTask(
                        0x5012, Priority.HIGH,
                        m(ElectricityMeter.ChannelId.ACTIVE_POWER,    new FloatDoublewordElement(0x5012).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(0x5014).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(0x5016).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(0x5018).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3)
                ),

                // REACTIVE POWER (kvar) total + L1/L2/L3
                new FC3ReadRegistersTask(
                        0x501A, Priority.HIGH,
                        m(ElectricityMeter.ChannelId.REACTIVE_POWER,    new FloatDoublewordElement(0x501A).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(ElectricityMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(0x501C).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(ElectricityMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(0x501E).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(ElectricityMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(0x5020).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3)
                ),

                // APPARENT POWER (kVA) total + L1/L2/L3
                new FC3ReadRegistersTask(
                        0x5022, Priority.HIGH,
                        m(Pro380modct.ChannelId.APPARENT_POWER,    new FloatDoublewordElement(0x5022).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(Pro380modct.ChannelId.APPARENT_POWER_L1, new FloatDoublewordElement(0x5024).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(Pro380modct.ChannelId.APPARENT_POWER_L2, new FloatDoublewordElement(0x5026).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3),
                        m(Pro380modct.ChannelId.APPARENT_POWER_L3, new FloatDoublewordElement(0x5028).wordOrder(WordOrder.MSWLSW), ElementToChannelConverter.SCALE_FACTOR_3)
                )
        );

    }


	@Override
	public String debugLog() {
		return this.channel(ElectricityMeter.ChannelId.VOLTAGE_L1).value().toString()
                + " / " + this.channel(ElectricityMeter.ChannelId.CURRENT).value().toString()
                + " / " + this.channel(ElectricityMeter.ChannelId.ACTIVE_POWER).value().toString()
                + " / " + this.channel(ElectricityMeter.ChannelId.REACTIVE_POWER).value().toString()
                + " / " + this.channel(Pro380modct.ChannelId.APPARENT_POWER).value().toString();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}
}
