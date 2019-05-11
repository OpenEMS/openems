package io.openems.edge.controller.api.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.SetChannelValueRequest;
import io.openems.common.session.User;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.ComponentManager;

/**
 * Takes care of continuously writing channels till a timeout. This class is
 * used in all Api-Controllers.
 *
 * @author stefan.feilmeier
 */
public class ApiWorker {

	private static final Logger log = LoggerFactory.getLogger(ApiWorker.class);
	public static final int DEFAULT_TIMEOUT_SECONDS = 10;

	/**
	 * Holds the mapping between WriteChannel and the value that it should be set
	 * to.
	 */
	private final Map<WriteChannel<?>, WriteObject> values = new HashMap<>();

	private final ScheduledExecutorService executor;
	private ScheduledFuture<?> future = null;

	private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

	public ApiWorker() {
		this.executor = Executors.newSingleThreadScheduledExecutor();
	}

	public void addValue(WriteChannel<?> channel, WriteObject writeObject) {
		log.info("Set [" + channel.address() + "] to [" + writeObject.valueToString() + "] via API. Timeout is ["
				+ this.timeoutSeconds + "s]");
		this.resetTimeout();
		synchronized (this.values) {
			this.values.put(channel, writeObject);
		}
	}

	/**
	 * Adds a value via JSON-RPC SetChannelValueRequest.
	 * 
	 * @param user    the authenticated User
	 * @param request the Request
	 * @return success
	 * @throws OpenemsNamedException    on error
	 * @throws IllegalArgumentException on error
	 */
	public CompletableFuture<JsonrpcResponseSuccess> handleSetChannelValueRequest(ComponentManager componentManager,
			User user, SetChannelValueRequest request) throws IllegalArgumentException, OpenemsNamedException {
		// check for writable channel
		Channel<?> channel = componentManager.getChannel(request.getChannelAddress());
		if (!(channel instanceof WriteChannel<?>)) {
			throw new OpenemsException("[" + channel + "] is not a Write Channel");
		}

		// parse value
		Object value;
		if (request.getValue().isJsonNull()) {
			value = null;
		} else {
			value = JsonUtils.getAsBestType(request.getValue());
		}

		// set value
		this.addValue((WriteChannel<?>) channel, new WritePojo(value));

		return CompletableFuture.completedFuture(new GenericJsonrpcResponseSuccess(request.getId()));
	}

	private synchronized void resetTimeout() {
		if (this.future != null) {
			this.future.cancel(false);
		}
		if (this.timeoutSeconds > 0) {
			this.future = this.executor.schedule(() -> {
				/**
				 * This worker takes care to clear the values list if there is no change within
				 * the timeout
				 */
				synchronized (this.values) {
					for (Entry<WriteChannel<?>, WriteObject> entry : this.values.entrySet()) {
						log.info("API timeout for channel [" + entry.getKey().address() + "] after ["
								+ this.timeoutSeconds + "s]");
						entry.getValue().notifyTimeout();
					}
					this.values.clear();
				}
			}, this.timeoutSeconds, TimeUnit.SECONDS);
		}
	}

	/**
	 * Sets the timeout in seconds. Default is 60, which means that for 60 seconds
	 * in each cycle a value is rewritten to the WriteChannel. If set to '0',
	 * timeout is deactivated.
	 *
	 * @param timeoutSeconds the timeout for this ApiWorker
	 */
	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
		this.resetTimeout();
	}

	/**
	 * Sets the channels. This method is called by the run() method of the
	 * Controller
	 * 
	 * @throws OpenemsNamedException on error
	 */
	public void run() throws OpenemsNamedException {
		OpenemsNamedException anExceptionHappened = null;
		synchronized (this.values) {
			for (Entry<WriteChannel<?>, WriteObject> entry : this.values.entrySet()) {
				WriteChannel<?> channel = entry.getKey();
				WriteObject writeObject = entry.getValue();
				try {
					log.info("Set Channel [" + channel.address() + "] to Value [" + writeObject.valueToString() + "]");
					writeObject.setNextWriteValue(channel);
					writeObject.notifySuccess();
				} catch (OpenemsException e) {
					log.error("Unable to set Channel [" + channel.address() + "] to Value ["
							+ writeObject.valueToString() + "]: " + e.getMessage());
					writeObject.notifyError(e);
					anExceptionHappened = e;
				}
			}
		}
		if (anExceptionHappened != null) {
			throw anExceptionHappened;
		}
	}
}
