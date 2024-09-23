package io.openems.edge.controller.api.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.base.GenericJsonrpcResponseSuccess;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.jsonrpc.request.SetChannelValueRequest;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.FunctionUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.StringReadChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.User;

/**
 * Takes care of continuously writing channels till a timeout. This class is
 * used in all Api-Controllers.
 *
 * @author stefan.feilmeier
 */
public class ApiWorker {

	public static final int DEFAULT_TIMEOUT_SECONDS = 10;

	private final Logger log = LoggerFactory.getLogger(ApiWorker.class);

	private final AbstractOpenemsComponent parent;

	/**
	 * Debug information about writes to channels is sent to this channel.
	 */
	private StringReadChannel logChannel = null;

	/**
	 * Holds the mapping between WriteChannel and the value that it should be set
	 * to.
	 */
	private final Map<WriteChannel<?>, WriteObject> values = new HashMap<>();

	private final ScheduledExecutorService executor;
	private ScheduledFuture<?> future = null;

	private int timeoutSeconds = DEFAULT_TIMEOUT_SECONDS;

	/**
	 * Handles write-only channel overriding.
	 */
	public record WriteHandler(Consumer<Entry<WriteChannel<?>, WriteObject>> handleWrites, //
			Consumer<Status> setOverrideStatus, //
			Runnable handleTimeout) {
	}

	private WriteHandler writeHandler;

	public ApiWorker(AbstractOpenemsComponent parent) {
		this(parent, new WriteHandler(FunctionUtils::doNothing, //
				FunctionUtils::doNothing, FunctionUtils::doNothing));
	}

	public ApiWorker(AbstractOpenemsComponent parent, WriteHandler writeHandler) { //
		this.parent = parent;
		this.writeHandler = writeHandler;
		this.executor = Executors.newSingleThreadScheduledExecutor();
	}

	/**
	 * Sets the Channel that should be used to log debug information about writes to
	 * channels.
	 *
	 * @param logChannel a {@link StringReadChannel}
	 */
	public void setLogChannel(StringReadChannel logChannel) {
		this.logChannel = logChannel;
	}

	/**
	 * Adds a value to the write-pipeline. The values are then set in the next
	 * execution of {@link #run()}, until the timeout is reached.
	 *
	 * @param channel     the {@link WriteChannel}
	 * @param writeObject the {@link WriteObject}
	 */
	public void addValue(WriteChannel<?> channel, WriteObject writeObject) {
		this.resetTimeout();
		synchronized (this.values) {
			if (writeObject.isNull()) {
				// set null -> remove write-value
				OpenemsComponent.logInfo(this.parent, this.log,
						"Set [" + channel.address() + "] to [" + writeObject.valueToString() + "] via API");
				this.values.remove(channel);
			} else {
				// set write-value
				OpenemsComponent.logInfo(this.parent, this.log, "Set [" + channel.address() + "] to ["
						+ writeObject.valueToString() + "] via API. Timeout is [" + this.timeoutSeconds + "s]");
				this.values.put(channel, writeObject);
			}
		}
	}

	/**
	 * Adds a value via JSON-RPC {@link SetChannelValueRequest}.
	 *
	 * @param componentManager the {@link ComponentManager}
	 * @param user             the authenticated {@link User}
	 * @param request          the Request
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
			if (value instanceof String && ((String) value).isEmpty()
					&& channel.channelId().doc().getType() != OpenemsType.STRING) {
				// Allow non-string Channels to be set to 'UNDEFINED' using an empty string
				value = null;
			}
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
						OpenemsComponent.logInfo(this.parent, this.log, "API timeout for channel ["
								+ entry.getKey().address() + "] after [" + this.timeoutSeconds + "s]");
						entry.getValue().notifyTimeout();
					}
					this.writeHandler.setOverrideStatus.accept(Status.INACTIVE);
					this.writeHandler.handleTimeout.run();
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
		List<String> logs = new ArrayList<>();
		synchronized (this.values) {
			for (Entry<WriteChannel<?>, WriteObject> entry : this.values.entrySet()) {
				WriteChannel<?> channel = entry.getKey();
				var writeObject = entry.getValue();
				try {
					OpenemsComponent.logInfo(this.parent, this.log,
							"Set Channel [" + channel.address() + "] to Value [" + writeObject.valueToString() + "]");
					writeObject.setNextWriteValue(channel);
					writeObject.notifySuccess();

					logs.add(channel.address() + ":" + writeObject.valueToString());

					this.writeHandler.handleWrites.accept(entry);
					this.writeHandler.setOverrideStatus.accept(Status.ACTIVE);
				} catch (OpenemsException e) {
					OpenemsComponent.logError(this.parent, this.log, "Unable to set Channel [" + channel.address()
							+ "] to Value [" + writeObject.valueToString() + "]: " + e.getMessage());
					logs.add(channel.address() + ":" + writeObject.valueToString() + "-ERROR:" + e.getMessage());
					writeObject.notifyError(e);
					anExceptionHappened = e;
				}
			}
		}
		if (this.logChannel != null) {
			this.logChannel.setNextValue(String.join("|", logs));
		}
		if (anExceptionHappened != null) {
			throw anExceptionHappened;
		}
	}
}
