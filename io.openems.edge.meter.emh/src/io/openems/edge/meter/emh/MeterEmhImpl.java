
package io.openems.edge.meter.emh;

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
import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.element.SignedDoublewordElement;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.EMH", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterEmhImpl extends AbstractOpenemsModbusComponent
		implements MeterEmh, ElectricityMeter, ModbusComponent, OpenemsComponent, ModbusSlave {

	private Config config;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public MeterEmhImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), //
				MeterEmh.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
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
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		ModbusProtocol modbusProtocol = new ModbusProtocol(this, //
				new FC3ReadRegistersTask(8, Priority.HIGH, //
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L1, new SignedDoublewordElement(8),
								this.applyValueFromFactor),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L2, new SignedDoublewordElement(10),
								this.applyValueFromFactor),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER_L3, new SignedDoublewordElement(12),
								this.applyValueFromFactor),
						m(ElectricityMeter.ChannelId.ACTIVE_POWER, new SignedDoublewordElement(14),
								this.applyValueFromFactor),
						m(ElectricityMeter.ChannelId.REACTIVE_POWER, new SignedDoublewordElement(16),
								this.applyValueFromFactor)));

		if (this.config.invert()) {
			modbusProtocol.addTask(new FC3ReadRegistersTask(20, Priority.HIGH, //
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new SignedDoublewordElement(20),
							this.applyValueFromFactor),
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new SignedDoublewordElement(22),
							this.applyValueFromFactor)));
		} else {
			modbusProtocol.addTask(new FC3ReadRegistersTask(20, Priority.HIGH, //
					m(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY, new SignedDoublewordElement(20),
							this.applyValueFromFactor),
					m(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, new SignedDoublewordElement(22),
							this.applyValueFromFactor)));
		}
		return modbusProtocol;
	}

	private final ElementToChannelConverter applyValueFromFactor = new ElementToChannelConverter(//
			value -> {
				if (value == null) {
					return null;
				}
				long intValue = (Long) value;
				if (intValue == -10_000) {
					return 0; // ignore '-10_000'
				}
				return intValue * this.config.converterFactor(); //
			}, //
			value -> value);

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ElectricityMeter.getModbusSlaveNatureTable(accessMode),
				ModbusSlaveNatureTable.of(MeterEmh.class, accessMode, 100) //
						.build());
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}
}
