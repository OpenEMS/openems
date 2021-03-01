package io.openems.common.websocket;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractWebsocket<T extends WsData> {

	private final Logger log = LoggerFactory.getLogger(AbstractWebsocket.class);
	private final String name;

	/**
	 * Shared {@link ExecutorService}. Configuration is equal to
	 * Executors.newCachedThreadPool(), but with DiscardOldestPolicy.
	 */
	private final ThreadPoolExecutor executor;

	/*
	 * This Executor is used if Debug-Mode is activated.
	 */
	private final ScheduledExecutorService debugLogExecutor;

	/**
	 * Creates an empty WsData object that is attached to the WebSocket as early as
	 * possible
	 * 
	 * @return
	 */
	protected abstract T createWsData();

	/**
	 * Callback for internal error
	 * 
	 * @return
	 */
	protected abstract OnInternalError getOnInternalError();

	/**
	 * Callback for websocket OnOpen event
	 * 
	 * @return
	 */
	protected abstract OnOpen getOnOpen();

	/**
	 * Callback for JSON-RPC request
	 * 
	 * @return
	 */
	protected abstract OnRequest getOnRequest();

	/**
	 * Callback for JSON-RPC notification
	 * 
	 * @return
	 */
	protected abstract OnNotification getOnNotification();

	/**
	 * Callback for websocket error
	 * 
	 * @return
	 */
	protected abstract OnError getOnError();

	/**
	 * Callback for websocket OnClose event
	 * 
	 * @return
	 */
	protected abstract OnClose getOnClose();

	/**
	 * Construct this {@link AbstractWebsocket}.
	 * 
	 * @param name      a name that is used to identify log messages
	 * @param poolSize  number of threads dedicated to handle the tasks
	 * @param debugMode activate a regular debug log about the state of the tasks
	 */
	public AbstractWebsocket(String name, int poolSize, boolean debugMode) {
		this.name = name;
		this.executor = new ThreadPoolExecutor(poolSize, poolSize, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		if (debugMode) {
			this.debugLogExecutor = Executors.newSingleThreadScheduledExecutor();
			this.debugLogExecutor.scheduleWithFixedDelay(() -> {
				this.logInfo(this.log,
						String.format("[monitor] Pool: %d, Active: %d, Pending: %d, Completed: %d",
								this.executor.getPoolSize(), //
								this.executor.getActiveCount(), //
								this.executor.getQueue().size(), //
								this.executor.getCompletedTaskCount())); //
			}, 10, 10, TimeUnit.SECONDS);
		} else {
			this.debugLogExecutor = null;
		}
	}

	/**
	 * Gets the internal name of this websocket client/server
	 * 
	 * @return
	 */
	public String getName() {
		return name;
	}

	protected void start() {

	}

	public void stop() {
		// Shutdown executor
		if (this.executor != null) {
			try {
				this.executor.shutdown();
				this.executor.awaitTermination(5, TimeUnit.SECONDS);
			} catch (InterruptedException e) {
				this.logWarn(this.log, "tasks interrupted");
			} finally {
				if (!this.executor.isTerminated()) {
					this.logWarn(this.log, "cancel non-finished tasks");
				}
				this.executor.shutdownNow();
			}
		}
		if (this.debugLogExecutor != null) {
			this.debugLogExecutor.shutdown();
		}
	}

	/**
	 * Execute a {@link Runnable} using the shared {@link ExecutorService}.
	 * 
	 * @param command the {@link Runnable}
	 */
	protected void execute(Runnable command) {
		this.executor.execute(command);
	}

	/**
	 * Handles an internal Error asynchronously
	 * 
	 * @param e
	 */
	protected void handleInternalErrorAsync(Exception e) {
		this.execute(new OnInternalErrorHandler(this.getOnInternalError(), e));
	}

	/**
	 * Handles an internal Error synchronously
	 * 
	 * @param e
	 */
	protected void handleInternalErrorSync(Exception e, String wsDataString) {
		this.getOnInternalError().run(e, wsDataString);
	}

	/**
	 * Log a info message.
	 * 
	 * @param log     a Logger instance
	 * @param message the message
	 */
	protected abstract void logInfo(Logger log, String message);

	/**
	 * Log a warn message.
	 * 
	 * @param log     a Logger instance
	 * @param message the message
	 */
	protected abstract void logWarn(Logger log, String message);

}
