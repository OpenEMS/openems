package io.openems.edge.meter.pqplus.umd96;

import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_3;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

/**
 * Implements the PQ Plus UMD 96 meter.
 *
 * <ul>
 * <li>https://www.pq-plus.de/produkte/hardwarekomponenten/umd-96/
 * <li>https://www.pq-plus.de/site/assets/files/2795/pqplus-com-protokoll-modbus_3_0.pdf
 * </ul>
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.PqPlus.UMD96", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MeterPqplusUmd96Impl extends AbstractOpenemsModbusComponent
		implements MeterPqplusUmd96, SymmetricMeter, AsymmetricMeter, ModbusComponent, OpenemsComponent {

	private MeterType meterType = MeterType.PRODUCTION;

	@Reference
	private ConfigurationAdmin cm;

	public MeterPqplusUmd96Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				MeterPqplusUmd96.ChannelId.values() //
		);
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		this.meterType = config.type();

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
	public MeterType getMeterType() {
		return this.meterType;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		return new ModbusProtocol(this, //
				// Frequency
				new FC3ReadRegistersTask(0x1004, Priority.LOW, //
						m(SymmetricMeter.ChannelId.FREQUENCY, new FloatDoublewordElement(0x1004), SCALE_FACTOR_3)),
				// Voltages
				new FC3ReadRegistersTask(0x1100, Priority.HIGH, //
						m(new FloatDoublewordElement(0x1100)) //
								.m(AsymmetricMeter.ChannelId.VOLTAGE_L1, SCALE_FACTOR_3) //
								.m(SymmetricMeter.ChannelId.VOLTAGE, SCALE_FACTOR_3) //
								.build(), //
						m(AsymmetricMeter.ChannelId.VOLTAGE_L2, new FloatDoublewordElement(0x1102), SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.VOLTAGE_L3, new FloatDoublewordElement(0x1104), SCALE_FACTOR_3)),
				// Currents
				new FC3ReadRegistersTask(0x1200, Priority.HIGH, //
						m(new FloatDoublewordElement(0x1200)).m(AsymmetricMeter.ChannelId.CURRENT_L1, SCALE_FACTOR_3) //
								.m(SymmetricMeter.ChannelId.CURRENT, SCALE_FACTOR_3) //
								.build(), //
						m(AsymmetricMeter.ChannelId.CURRENT_L2, new FloatDoublewordElement(0x1202), //
								SCALE_FACTOR_3),
						m(AsymmetricMeter.ChannelId.CURRENT_L3, new FloatDoublewordElement(0x1204), //
								SCALE_FACTOR_3)),
				// Power values
				new FC3ReadRegistersTask(0x1314, Priority.HIGH, //
						m(SymmetricMeter.ChannelId.ACTIVE_POWER, new FloatDoublewordElement(0x1314)),
						m(SymmetricMeter.ChannelId.REACTIVE_POWER, new FloatDoublewordElement(0x1316)), //
						new DummyRegisterElement(0x1318, 0x131F), //
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L1, new FloatDoublewordElement(0x1320)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L2, new FloatDoublewordElement(0x1322)),
						m(AsymmetricMeter.ChannelId.ACTIVE_POWER_L3, new FloatDoublewordElement(0x1324)),
						new DummyRegisterElement(0x1326, 0x1327), //
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L1, new FloatDoublewordElement(0x1328)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L2, new FloatDoublewordElement(0x132A)),
						m(AsymmetricMeter.ChannelId.REACTIVE_POWER_L3, new FloatDoublewordElement(0x132C)))//
		);

	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}
}
