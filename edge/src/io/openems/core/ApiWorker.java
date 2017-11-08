package io.openems.core;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.exception.WriteChannelException;

/**
 * Takes care of continuously writing channels till a timeout. This class is used in all Api-Controllers.
 *
 * @author stefan.feilmeier
 */
public class ApiWorker {

	private final static Logger log = LoggerFactory.getLogger(ApiWorker.class);
	private final static int DEFAULT_TIMEOUT_SECONDS = 10;

	private final Map<WriteChannel<?>, Object> values = new HashMap<>();
	private final ScheduledExecutorService executor;
	private ScheduledFuture<?> future = null;

	private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

	public ApiWorker() {
		this.executor = Executors.newSingleThreadScheduledExecutor();
	}

	public void addValue(WriteChannel<?> channel, Object value) {
		log.info("Set [" + channel.address() + "] to [" + value + "] via API. Timeout is [" + this.timeoutSeconds + "s]");
		this.resetTimeout();

		synchronized (this.values) {
			this.values.put(channel, value);
		}
	}

	private synchronized void resetTimeout() {
		if (this.future != null) {
			this.future.cancel(false);
		}
		if (this.timeoutSeconds > 0) {
			this.future = this.executor.schedule(() -> {
				/**
				 * This worker takes care to clear the values list if there is no change within the timeout
				 */
				synchronized (this.values) {
					for(Channel channel : this.values.keySet()) {
						log.info("API timeout for channel [" + channel.address() + "] after [" + this.timeoutSeconds + "s]");
					}
					this.values.clear();
				}
			}, this.timeoutSeconds, TimeUnit.SECONDS);
		}
	}

	/**
	 * Sets the timeout in seconds. Default is 60. If set to '0', timeout is deactivated.
	 *
	 * @param timeoutSeconds
	 */
	public void setTimeoutSeconds(int timeoutSeconds) {
		this.timeoutSeconds = timeoutSeconds;
		this.resetTimeout();
	}

	/**
	 * Sets the channels
	 */
	public void writeChannels() {
		synchronized (this.values) {
			for (Entry<WriteChannel<?>, Object> entry : this.values.entrySet()) {
				WriteChannel<?> channel = entry.getKey();
				Object value = entry.getValue();
				try {
					log.info("Set Channel [" + channel.address() + "] to Value [" + value + "]");
					channel.pushWriteFromObject(value);
				} catch (WriteChannelException e) {
					log.error("Unable to set Channel [" + channel.address() + "] to Value [" + value + "]: "
							+ e.getMessage());
				}
			}
		}
	}
}
