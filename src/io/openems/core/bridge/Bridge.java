package io.openems.core.bridge;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.device.Device;
import io.openems.api.thing.Thing;
import io.openems.core.utilities.Mutex;

public abstract class Bridge extends Thread implements Thing {
	public final static String THINGID_PREFIX = "_bridge";
	private static int instanceCounter = 0;
	private final static Logger log = LoggerFactory.getLogger(Bridge.class);
	protected Device[] devices = null;
	private final AtomicBoolean initialize = new AtomicBoolean(true);

	private final AtomicBoolean initialized = new AtomicBoolean(false);

	private Mutex initializedMutex = new Mutex(false);

	/**
	 * Initialize the Thread with a name
	 *
	 * @param name
	 */
	public Bridge() {
		super(THINGID_PREFIX + instanceCounter++);
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

	public void setDevices(Device... devices) {
		this.devices = devices;
	}

	/**
	 * Triggers a call of {@link initialization()} in the next loop. Call this, if the configuration changes.
	 */
	public void triggerInitialize() {
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
	protected abstract void forever() throws Throwable;

	/**
	 * This method is called once before {@link forever()} and every time after {@link restart()} method was called. Use
	 * it to (re)initialize everything.
	 *
	 * @return false on initialization error
	 */
	protected abstract boolean initialize();
}
