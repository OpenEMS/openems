package io.openems.edge.meter.socomec;

import java.util.concurrent.CompletableFuture;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.StringWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedQuadruplewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Meter.Socomec", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SocomecMeterImpl extends AbstractOpenemsModbusComponent
		implements SymmetricMeter, AsymmetricMeter, OpenemsComponent, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(SocomecMeterImpl.class);

	private final ModbusProtocol modbusProtocol;

	private Config config;

	@Reference
	private ConfigurationAdmin cm;

	public SocomecMeterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				SymmetricMeter.ChannelId.values(), //
				AsymmetricMeter.ChannelId.values(), //
				SocomecMeter.ChannelId.values() //
		);
		this.modbusProtocol = new ModbusProtocol(this);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm, "Modbus",
				config.modbus_id());
		this.config = config;
		this.identifySocomecMeter();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public MeterType getMeterType() {
		return this.config.type();
	}

	@Override
	protected final ModbusProtocol defineModbusProtocol() {
		return this.modbusProtocol;
	}

	/**
	 * Identifies the Socomec meter and applies the appropriate modbus protocol.
	 */
	private void identifySocomecMeter() {
		// Search for Socomec identifier register. Needs to be "SOCO".
		this.readELementOnce(new UnsignedQuadruplewordElement(0xC350)).thenAccept(value -> {
			if (value != 0x0053004F0043004FL /* SOCO */) {
				this.channel(SocomecMeter.ChannelId.NO_SOCOMEC_METER).setNextValue(true);
				return;
			}
			// Found Socomec meter
			this.readELementOnce(new StringWordElement(0xC38A, 8)).thenAccept(name -> {
				switch (name) {
				case "Countis E24":
					this.logInfo(this.log, "Identified Socomec Countis E24 meter");
					this.protocolCountisE24();
					break;
				default:
					this.channel(SocomecMeter.ChannelId.UNKNOWN_SOCOMEC_METER).setNextValue(true);
				}
			});
		});
	}

	/**
	 * Applies the modbus protocol for Socomec Countis E24.
	 */
	private void protocolCountisE24() {
		// TODO
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().value().asString();
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				SymmetricMeter.getModbusSlaveNatureTable(accessMode), //
				AsymmetricMeter.getModbusSlaveNatureTable(accessMode) //
		);
	}

	/**
	 * Reads given Element once from Modbus.
	 * 
	 * @param <T>     the Type of the element
	 * @param element the element
	 * @return a future value, e.g. a integer
	 */
	private <T> CompletableFuture<T> readELementOnce(AbstractModbusElement<T> element) {
		// Prepare result
		final CompletableFuture<T> result = new CompletableFuture<T>();

		// Activate task
		final Task task = new FC3ReadRegistersTask(element.getStartAddress(), Priority.HIGH, element);
		this.modbusProtocol.addTask(task);

		// Register listener for element
		element.onUpdateCallback(value -> {
			if (value == null) {
				// try again
				return;
			}
			// do not try again
			this.modbusProtocol.removeTask(task);
			result.complete(value);
		});

		return result;
	}
}
