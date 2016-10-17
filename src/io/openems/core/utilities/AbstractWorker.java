package io.openems.core.utilities;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.thing.Thing;

public abstract class AbstractWorker extends Thread implements Thing {
	protected final Logger log;
	private final AtomicBoolean initialize = new AtomicBoolean(true);
	private final AtomicBoolean initialized = new AtomicBoolean(false);
	private Mutex initializedMutex = new Mutex(false);

	/**
	 * Initialize the Thread with a name
	 *
	 * @param name
	 */
	public AbstractWorker(String name) {
		log = LoggerFactory.getLogger(this.getClass());
		this.setName(name);
		activate(); // TODO: remove for OSGi
	}

	public void activate() {
		this.start();
	}

	@Override
	public String getThingId() {
		return getName();
	}

	/**
	 * Executes the Thread. Calls {@link forever} till the Thread gets interrupted.
	 */
	@Override
	public final void run() {
		this.initialize.set(true);
		while (!isInterrupted()) {
			try {
				while (initialize.get()) {
					boolean initSuccessful = initialize();
					if (initSuccessful) {
						initialized.set(true);
						initializedMutex.release();
						initialize.set(false);
					} else {
						initializedMutex.awaitOrTimeout(10, TimeUnit.SECONDS);
					}
				}
				try {
					forever();
				} catch (Throwable e) {
					log.error("Bridge execution failed! Trying to initialize again: {}", e.getMessage());
					forever();
				}
				Thread.sleep(1000); // TODO add cycle time
			} catch (Throwable e) {
				System.out.println("BridgeWorker-Exception! " + e.getMessage());
				e.printStackTrace();
			}
		}
		dispose();
		System.out.println("BridgeWorker was interrupted. Exiting gracefully...");
	};

	/**
	 * Triggers a call of {@link initialization()} in the next loop. Call this, if the configuration changes.
	 */
	public final void triggerInitialize() {
		initialize.set(true);
		initializedMutex.release();
	}

	/**
	 * This method is called when the Thread stops. Use it to close resources.
	 */
	protected abstract void dispose();

	/**
	 * This method is called in a loop forever until the Thread gets interrupted.
	 */
	protected abstract void forever();

	/**
	 * This method is called once before {@link forever()} and every time after {@link restart()} method was called. Use
	 * it to (re)initialize everything.
	 *
	 * @return false on initialization error
	 */
	protected abstract boolean initialize();
}
