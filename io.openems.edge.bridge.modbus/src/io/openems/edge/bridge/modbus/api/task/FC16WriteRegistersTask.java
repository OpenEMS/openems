package io.openems.edge.bridge.modbus.api.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.WriteMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.procimg.Register;

import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.LogVerbosity;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.ModbusRegisterElement;

/**
 * Implements a Write Holding Registers Task, using Modbus function code 16
 * (http://www.simplymodbus.ca/FC16.htm).
 */
public class FC16WriteRegistersTask
		extends AbstractWriteTask<WriteMultipleRegistersRequest, WriteMultipleRegistersResponse> {

	private final Logger log = LoggerFactory.getLogger(FC16WriteRegistersTask.class);

	public FC16WriteRegistersTask(int startAddress, ModbusElement... elements) {
		super("FC16WriteRegisters", WriteMultipleRegistersResponse.class, startAddress, elements);
	}

	@Override
	public ExecuteState execute(AbstractModbusBridge bridge) {
		var requests = mergeWriteRegisters(this.elements, message -> this.log.warn(message)).stream() //
				.map(e -> new WriteMultipleRegistersRequest(e.startAddress(), e.getRegisters())) //
				.toList();

		if (requests.isEmpty()) {
			return ExecuteState.NO_OP;
		}

		boolean hasError = false;
		for (var request : requests) {
			try {
				this.executeRequest(bridge, request);

			} catch (Exception e) {
				// On error a log message has already been logged

				// Invalidate Elements
				Stream.of(this.elements).forEach(el -> el.invalidate(bridge));
				hasError = true;
			}
		}

		if (hasError) {
			return ExecuteState.ERROR;
		} else {
			return ExecuteState.OK;
		}
	}

	/**
	 * Combine WriteRegisters without holes in between.
	 * 
	 * @param elements the {@link ModbusElement}s
	 * @param logWarn  {@link Consumer} to log a warning
	 * @return a list of CombinedWriteRegisters
	 */
	protected static List<MergedWriteRegisters> mergeWriteRegisters(ModbusElement[] elements,
			Consumer<String> logWarn) {
		final var writes = new ArrayList<MergedWriteRegisters>();
		for (var element : elements) {
			if (element instanceof ModbusRegisterElement<?, ?> e) {
				var registers = e.getNextWriteValueAndReset();
				if (registers != null) {
					// found value registers -> add to 'writes'
					final MergedWriteRegisters write;
					if (writes.isEmpty()) {
						// no writes created yet
						write = MergedWriteRegisters.of(e.startAddress);
						writes.add(write);
					} else {
						var lastWrite = writes.get(writes.size() - 1);
						if (lastWrite.getLastAddress() + 1 != e.startAddress) {
							// there is a hole between last element and current element
							write = MergedWriteRegisters.of(e.startAddress);
							writes.add(write);
						} else {
							// no hole -> combine writes
							write = writes.get(writes.size() - 1);
						}
					}
					write.add(registers);
				}
			} else {
				logWarn.accept(
						"Unable to execute Write for ModbusElement [" + element + "]: No ModbusRegisterElement!");
			}
		}
		return writes;
	}

	protected static record MergedWriteRegisters(int startAddress, List<Register> registers) {
		public static MergedWriteRegisters of(int startAddress) {
			return new MergedWriteRegisters(startAddress, new ArrayList<>());
		}

		public void add(Register... registers) {
			Collections.addAll(this.registers, registers);
		}

		public Register[] getRegisters() {
			return this.registers.toArray(new Register[this.registers.size()]);
		}

		public int getLastAddress() {
			return this.startAddress + this.registers.size() - 1;
		}
	}

	@Override
	protected String payloadToString(WriteMultipleRegistersRequest request) {
		return ModbusUtils.registersToHexString(request.getRegisters());
	}

	@Override
	protected String toLogMessage(LogVerbosity logVerbosity, WriteMultipleRegistersRequest request,
			WriteMultipleRegistersResponse response, Exception exception) {
		// Read StartAddress and Length from the actual Sub-Request
		return this.toLogMessage(logVerbosity, request.getReference(), request.getWordCount(), request, response,
				exception);
	}
}
