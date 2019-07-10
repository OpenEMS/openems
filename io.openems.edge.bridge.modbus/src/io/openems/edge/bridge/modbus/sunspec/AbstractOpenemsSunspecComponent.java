package io.openems.edge.bridge.modbus.sunspec;

import java.util.concurrent.CompletableFuture;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.Task;
import io.openems.edge.common.taskmanager.Priority;

/**
 * This class provides a generic implementation of SunSpec ModBus protocols.
 */
public abstract class AbstractOpenemsSunspecComponent extends AbstractOpenemsModbusComponent {

	private final ModbusProtocol modbusProtocol;

	protected AbstractOpenemsSunspecComponent(io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[][] furtherInitialChannelIds) {
		super(firstInitialChannelIds, furtherInitialChannelIds);
		this.modbusProtocol = new ModbusProtocol(this);
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId) {
		super.activate(context, id, alias, enabled, unitId, cm, modbusReference, modbusId);

		// Start the SunSpec read procedure...
		this.isSunSpec().thenAccept(isSunSpec -> {
			System.out.println("Is SunSpec? " + isSunSpec);
		});
	}

	@Override
	protected final ModbusProtocol defineModbusProtocol() {
		return this.modbusProtocol;
	}

	/**
	 * Validates that this device complies to SunSpec specification.
	 * 
	 * <p>
	 * Tests if first registers are 0x53756e53 ("SunS").
	 * 
	 * @return a future true if it is SunSpec; otherwise false
	 */
	private CompletableFuture<Boolean> isSunSpec() {
		final CompletableFuture<Boolean> result = new CompletableFuture<Boolean>();
		final AbstractModbusElement<?> element = new UnsignedDoublewordElement(40000);
		final Task task = new FC3ReadRegistersTask(40000, Priority.HIGH, element);
		element.onUpdateCallback(value -> {
			if (value == null) {
				// try again
				return;
			}
			// do not try again
			this.modbusProtocol.removeTask(task);
			if ((Long) value == 0x53756e53) {
				result.complete(true);
			} else {
				result.complete(false);
			}
		});
		this.modbusProtocol.addTask(task);
		return result;
	}

}
