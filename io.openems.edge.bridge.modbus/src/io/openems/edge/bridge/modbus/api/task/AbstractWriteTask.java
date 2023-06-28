package io.openems.edge.bridge.modbus.api.task;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractModbusBridge;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.common.taskmanager.Priority;

public abstract class AbstractWriteTask<//
		REQUEST extends ModbusRequest, //
		RESPONSE extends ModbusResponse> //
		extends AbstractTask<REQUEST, RESPONSE> implements WriteTask {

	public AbstractWriteTask(String name, Class<RESPONSE> responseClazz, int startAddress,
			ModbusElement<?>... elements) {
		super(name, responseClazz, startAddress, elements);
	}

	/**
	 * Priority for WriteTasks is by default always HIGH.
	 *
	 * @return {@link Priority#HIGH}
	 */
	@Override
	public Priority getPriority() {
		return Priority.HIGH;
	}

	public static abstract class Single<//
			REQUEST extends ModbusRequest, //
			RESPONSE extends ModbusResponse, //
			ELEMENT extends ModbusElement<?>> //
			extends AbstractWriteTask<REQUEST, RESPONSE> {

		protected final ELEMENT element;

		private final Logger log = LoggerFactory.getLogger(Single.class);

		public Single(String name, Class<RESPONSE> responseClazz, int startAddress, ELEMENT element) {
			super(name, responseClazz, startAddress, element);
			this.element = element;
		}

		@Override
		public final ExecuteState execute(AbstractModbusBridge bridge) {
			try {
				var request = this.createModbusRequest();
				if (request == null) {
					return ExecuteState.NO_OP;
				}

				this.executeRequest(bridge, request);
				return ExecuteState.OK;

			} catch (OpenemsException e) {
				this.logError(bridge, this.log, "Execute failed: " + e.getMessage());
				return ExecuteState.ERROR;
			}
		}

		/**
		 * Factory for a {@link ModbusRequest}.
		 * 
		 * @return a new {@link ModbusRequest}
		 * @throws OpenemsException on error
		 */
		protected abstract REQUEST createModbusRequest() throws OpenemsException;
	}
}
