package io.openems.edge.controller.api.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.WriteChannel;

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
	 */
	public void run() {
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
				}
			}
		}
	}
}
