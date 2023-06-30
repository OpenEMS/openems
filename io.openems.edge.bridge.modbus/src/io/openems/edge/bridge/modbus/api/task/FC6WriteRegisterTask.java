package io.openems.edge.bridge.modbus.api.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.AbstractWordElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;

public class FC6WriteRegisterTask extends AbstractTask implements WriteTask {

	private final Logger log = LoggerFactory.getLogger(FC6WriteRegisterTask.class);

	public FC6WriteRegisterTask(int startAddress, AbstractModbusElement<?> element) {
		super(startAddress, element);
	}

	@Override
	public int _execute(AbstractModbusBridge bridge) throws OpenemsException {
		var noOfWrittenRegisters = 0;
		ModbusElement<?> element = this.getElements()[0];

		if (element instanceof AbstractWordElement<?, ?>) {

			var valueOpt = ((AbstractWordElement<?, ?>) element).getNextWriteValueAndReset();
			if (valueOpt.isPresent()) {
				var registers = valueOpt.get();

				if (registers.length == 1 && registers[0] != null) {
					// found value -> write
					var register = registers[0];
					try {
						/*
						 * First try
						 */

						this.writeSingleRegister(bridge, this.getParent().getUnitId(), this.getStartAddress(),
								register);
						noOfWrittenRegisters = 1;
					} catch (OpenemsException | ModbusException e) {
						/*
						 * Second try: with new connection
						 */
						bridge.closeModbusConnection();
						try {
							this.writeSingleRegister(bridge, this.getParent().getUnitId(), this.getStartAddress(),
									register);
							noOfWrittenRegisters = 1;
						} catch (ModbusException e2) {
							throw new OpenemsException("Transaction failed: " + e.getMessage(), e2);
						}
					}
				} else {
					this.log.warn("Expecting exactly one register. Got [" + registers.length + "]");
				}
			}
		} else {
			this.log.warn("Unable to execute Write for ModbusElement [" + element + "]: No AbstractWordElement!");
		}
		return noOfWrittenRegisters;
	}

	@Override
	protected String getActiondescription() {
		return "FC6 WriteRegister";
	}

	private void writeSingleRegister(AbstractModbusBridge bridge, int unitId, int startAddress, Register register)
			throws ModbusException, OpenemsException {
		var request = new WriteSingleRegisterRequest(startAddress, register);
		var response = Utils.getResponse(request, unitId, bridge);

		// debug output
		switch (this.getLogVerbosity(bridge)) {
		case READS_AND_WRITES:
		case WRITES:
			bridge.logInfo(this.log, "FC6WriteRegister " //
					+ "[" + unitId + ":" + startAddress + "/0x" + Integer.toHexString(startAddress) + "]: " //
					+ String.format("%4s", Integer.toHexString(register.getValue())).replace(' ', '0'));
			break;
		case NONE:
			break;
		}

		if (!(response instanceof WriteSingleRegisterResponse)) {
			throw new OpenemsException("Unexpected Modbus response. Expected [WriteSingleRegisterResponse], got ["
					+ response.getClass().getSimpleName() + "]");
		}
	}
}
