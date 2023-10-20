package io.openems.edge.io.filipowski.analog.mr;

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
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.io.api.AnalogOutput;
import io.openems.edge.io.api.AnalogVoltageOutput;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "IO.Flipowski.MR-AO-1", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class IoFilipowskiMrAo1Impl extends AbstractOpenemsModbusComponent implements IoFilipowskiMrAo1, AnalogOutput,
		AnalogVoltageOutput, ModbusComponent, OpenemsComponent, ModbusSlave {

	private static final int MAXIMUM_VOLTAGE = 10_000; // mV
	private static final int PRECISION = 100; // mV
	private static final int OFFSET = 0; // mV

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private Config config = null;

	public IoFilipowskiMrAo1Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				AnalogOutput.ChannelId.values(), //
				AnalogVoltageOutput.ChannelId.values(), //
				IoFilipowskiMrAo1.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
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
		final var address = this.config.analogOutput().startAddress;
		return new ModbusProtocol(this, //

				// Output voltage
				new FC3ReadRegistersTask(address, Priority.HIGH, //
						m(AnalogVoltageOutput.ChannelId.SET_OUTPUT_VOLTAGE, new UnsignedWordElement(address),
								ElementToChannelConverter.SCALE_FACTOR_2) //
				),

				new FC6WriteRegisterTask(address, m(AnalogVoltageOutput.ChannelId.SET_OUTPUT_VOLTAGE,
						new UnsignedWordElement(address), ElementToChannelConverter.SCALE_FACTOR_2) //
				));
	}

	@Override
	public String debugLog() {
		return this.getDebugSetOutputVoltage().asOptional().map(t -> t + "mV").orElse("?");
	}

	@Override
	public Range range() {
		return new Range(OFFSET, PRECISION, MAXIMUM_VOLTAGE);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				AnalogVoltageOutput.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(IoFilipowskiMrAo1.class, accessMode, 100)//
						.build());
	}
}
