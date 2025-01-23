package io.openems.edge.bridge.modbus.api.task;

import io.openems.edge.bridge.modbus.api.element.ModbusElement;

/**
 * A Modbus 'ReadTask' is holding references to one or more Modbus
 * {@link ModbusElement}s which have register addresses in the same range. The
 * ReadTask handles the execution (query) on this range. @{link WriteTask}
 * inherits from ReadTask.
 */
public non-sealed interface ReadTask extends Task {
}
