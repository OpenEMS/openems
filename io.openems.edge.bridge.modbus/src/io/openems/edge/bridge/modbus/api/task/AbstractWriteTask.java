package io.openems.edge.bridge.modbus.api.task;

import java.util.function.Consumer;

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

	public AbstractWriteTask(String name, Consumer<ExecuteState> onExecute, Class<RESPONSE> responseClazz,
			int startAddress, ModbusElement... elements) {
		super(name, onExecute, responseClazz, startAddress, elements);
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

	@Override
	protected final String payloadToString(RESPONSE response) {
		return "";
	}

	public abstract static class Single<//
			REQUEST extends ModbusRequest, //
			RESPONSE extends ModbusResponse, //
			ELEMENT extends ModbusElement> //
			extends AbstractWriteTask<REQUEST, RESPONSE> {

		protected final ELEMENT element;

		private final Logger log = LoggerFactory.getLogger(Single.class);

		public Single(String name, Consumer<ExecuteState> onExecute, Class<RESPONSE> responseClazz, int startAddress,
				ELEMENT element) {
			super(name, onExecute, responseClazz, startAddress, element);
			this.element = element;
		}

		@Override
		public final ExecuteState execute(AbstractModbusBridge bridge) {
			var result = this._execute(bridge);
			this.onExecute.accept(result);
			return result;
		}

		private ExecuteState _execute(AbstractModbusBridge bridge) {
			this.callHooks(h -> h.preExecute(bridge, this));
			try {
				final REQUEST request;
				try {
					request = this.createModbusRequest();
				} catch (OpenemsException e) {
					logError(this.log, e, "Creating Modbus Request failed.");
					var state = new ExecuteState.Error(e);
					this.callHooks(h -> h.execute(bridge, this, state));
					return state;
				}

				if (request == null) {
					this.callHooks(h -> h.execute(bridge, this, ExecuteState.NO_OP));
					return ExecuteState.NO_OP;
				}

				try {
					var response = this.executeRequest(bridge, request);

					if (response == null) {
						// Bridge is stopped
						this.callHooks(h -> h.execute(bridge, this, ExecuteState.NO_OP));
						return ExecuteState.NO_OP;
					}

					this.callHooks(h -> h.execute(bridge, this, ExecuteState.OK));
					return ExecuteState.OK;

				} catch (Exception e) {
					// On error a log message has already been logged
					var state = new ExecuteState.Error(e);
					this.callHooks(h -> h.execute(bridge, this, state));
					return state;
				}
			} finally {
				this.callHooks(h -> h.postExecute(bridge, this));
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
