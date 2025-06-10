package io.openems.edge.bridge.onewire.impl;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.dalsemi.onewire.OneWireException;
import com.dalsemi.onewire.adapter.DSPortAdapter;
import com.dalsemi.onewire.adapter.PDKAdapterUSB;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.worker.AbstractImmediateWorker;
import io.openems.edge.bridge.onewire.jsonrpc.GetDeviceResponse;
import io.openems.edge.bridge.onewire.jsonrpc.GetDevicesRequest;

public class OneWireTaskWorker extends AbstractImmediateWorker {

	private final Logger log = LoggerFactory.getLogger(OneWireTaskWorker.class);
	private final CopyOnWriteArrayList<Consumer<DSPortAdapter>> tasks = new CopyOnWriteArrayList<>();
	private final BridgeOnewireImpl parent;
	private final String port;

	private DSPortAdapter _adapter = null;

	public OneWireTaskWorker(BridgeOnewireImpl parent, String port) {
		this.parent = parent;
		this.port = port;
	}

	@Override
	protected synchronized void forever() throws InterruptedException {
		DSPortAdapter adapter;
		try {
			adapter = this.getAdapter();
		} catch (OpenemsException e) {
			this.parent.logError(this.log, e.getMessage());
			Thread.sleep(5000);
			return;
		}

		for (Consumer<DSPortAdapter> task : this.tasks) {
			task.accept(adapter);
		}
	}

	/**
	 * Gets the DSPortAdapter and opens the port.
	 *
	 * @return the DSPortAdapter
	 * @throws OpenemsException on error
	 */
	private synchronized DSPortAdapter getAdapter() throws OpenemsException {
		if (this._adapter != null) {
			return this._adapter;
		}
		var adapter = new PDKAdapterUSB();
		try {
			if (adapter.selectPort(this.port)) {
				this.parent._setUnableToSelectPortFault(false);
				this._adapter = adapter;
				return this._adapter;

			}
			this.parent._setUnableToSelectPortFault(true);
			throw new OpenemsException("Unable to select port [" + this.port + "]");
		} catch (IllegalArgumentException | OneWireException e) {
			this.parent._setUnableToSelectPortFault(true);
			throw new OpenemsException("Unable to select port [" + this.port + "]: " + e.getMessage());
		}
	}

	@Override
	public synchronized void deactivate() {
		if (this._adapter != null) {
			try {
				this._adapter.freePort();
			} catch (OneWireException e) {
				this.parent.logError(this.log, e.getMessage());
			}
		}
		super.deactivate();
	}

	/**
	 * Adds a Task.
	 * 
	 * @param task the task
	 */
	public void addTask(Consumer<DSPortAdapter> task) {
		this.tasks.add(task);
	}

	/**
	 * Removes a Task.
	 * 
	 * @param task the task
	 */
	public void removeTask(Consumer<DSPortAdapter> task) {
		this.tasks.remove(task);
	}

	/**
	 * Handles a {@link GetDevicesRequest}.
	 * 
	 * @param request the {@link JsonrpcRequest}
	 * @return a {@link GetDeviceResponse}
	 * @throws OpenemsException on error
	 */
	public synchronized GetDeviceResponse handleGetDevicesRequest(JsonrpcRequest request) throws OpenemsException {
		var response = new GetDeviceResponse(request.getId());

		var adapter = this.getAdapter();
		try {
			while (adapter.findNextDevice()) {
				response.addDevice(//
						GetDeviceResponse.Device.from(//
								adapter.getDeviceContainer(adapter.getAddressAsLong())));
			}
		} catch (OneWireException e) {
			e.printStackTrace();
			throw new OpenemsException(e.getMessage());
		}
		return response;
	}
}
