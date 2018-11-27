package io.openems.edge.bridge.modbus.api.task;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterRequest;
import com.ghgande.j2mod.modbus.msg.WriteSingleRegisterResponse;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.element.AbstractModbusElement;
import io.openems.edge.bridge.modbus.api.element.AbstractWordElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;

public class FC6WriteRegisterTask extends AbstractTask implements WriteTask {

	private final Logger log = LoggerFactory.getLogger(FC6WriteRegisterTask.class);

	public FC6WriteRegisterTask(int startAddress, AbstractModbusElement<?> element) {
		super(startAddress, element);
	}

	@Override
	public void executeWrite(AbstractModbusBridge bridge) throws OpenemsException {
		ModbusElement<?> element = this.getElements()[0];

		if (element instanceof AbstractWordElement<?>) {

			Optional<Register[]> valueOpt = ((AbstractWordElement<?>) element).getNextWriteValue();
			if (valueOpt.isPresent()) {
				Register[] registers = valueOpt.get();

				if (registers.length == 1 && registers[0] != null) {
					// found value -> write
					try {
						/*
						 * First try
						 */

						this.writeSingleRegister(bridge, this.getParent().getUnitId(), this.getStartAddress(),
								registers[0]);
					} catch (OpenemsException | ModbusException e) {
						/*
						 * Second try: with new connection
						 */
						bridge.closeModbusConnection();
						try {
							this.writeSingleRegister(bridge, this.getParent().getUnitId(), this.getStartAddress(),
									registers[0]);
						} catch (ModbusException e2) {
							throw new OpenemsException("Transaction failed: " + e.getMessage(), e2);
						}
					}
				} else {
					log.warn("Expecting exactly one register. Got [" + registers.length + "]");
				}
			}
		} else {
			log.warn("Unable to execute Write for ModbusElement [" + element + "]: No AbstractWordElement!");
		}
	}

	@Override
	protected String getActiondescription() {
		return "FC6 Write Register";
	}

	private void writeSingleRegister(AbstractModbusBridge bridge, int unitId, int startAddress, Register register)
			throws ModbusException, OpenemsException {

		WriteSingleRegisterRequest request = new WriteSingleRegisterRequest(startAddress, register);
		ModbusResponse response = Utils.getResponse(request, unitId, bridge);

		if (!(response instanceof WriteSingleRegisterResponse)) {
			throw new OpenemsException("Unexpected Modbus response. Expected [WriteSingleRegisterResponse], got ["
					+ response.getClass().getSimpleName() + "]");
		}
	}
}
