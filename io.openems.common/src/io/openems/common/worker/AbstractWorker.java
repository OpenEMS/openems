package io.openems.common.worker;

import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.utils.Mutex;

/**
 * Defines a generic Worker Thread.
 * 
 * <p>
 * The business logic of the Worker is inside the {@link #forever()} method. It
 * is executed by default every {@link #getCycleTime()} seconds. Additionally
 * execution can be triggered by calling the {@link #triggerNextRun()} method.
 * 
 * <p>
 * If Cycle-Time is negative (e.g. by using
 * {@link #ALWAYS_WAIT_FOR_TRIGGER_NEXT_RUN}) the forever() method is called
 * only after triggering it.
 * 
 * <p>
 * If Cycle-Time is zero (e.g. by using {@link #DO_NOT_WAIT}), the forever()
 * method is always called immediately without any delay.
 */
public abstract class AbstractWorker {

	public final static int ALWAYS_WAIT_FOR_TRIGGER_NEXT_RUN = -1;
	public final static int DO_NOT_WAIT = 0;

	private final Logger log = LoggerFactory.getLogger(AbstractWorker.class);

	private final AtomicBoolean isTriggerRun = new AtomicBoolean(false);
	private final AtomicBoolean isStopped = new AtomicBoolean(false);
	private final Mutex cycleMutex = new Mutex(false);

	/**
	 * Initializes the worker and starts the worker thread
	 * 
	 * @param name
	 */
	public void activate(String name) {
		if (name != null) {
			this.worker.setName(name);
			this.worker.start();
		}
	}

	/**
	 * Stops the worker thread
	 */
	public void deactivate() {
		this.isStopped.set(true);
	}

	/**
	 * This method is called in a loop forever until the Thread gets interrupted.
	 */
	protected abstract void forever();

	/**
	 * Gets the cycleTime of this worker in [ms].
	 * <ul>
	 * <li>&gt; 0 sets the minimum execution time of one Cycle
	 * <li>= 0 never wait between two consecutive executions of forever()
	 * <li>&lt; 0 causes the Cycle to sleep forever until 'triggerNextRun()' is
	 * called
	 * </ul>
	 * 
	 * @return
	 */
	protected abstract int getCycleTime();

	/**
	 * Causes the Worker to interrupt sleeping and start again the forever() method
	 * immediately
	 */
	public void triggerNextRun() {
		this.isTriggerRun.set(true);
		this.worker.interrupt();
	}

	private final Thread worker = new Thread() {
		public void run() {
			long onWorkerExceptionSleep = 1; // seconds
			long cycleStart = System.currentTimeMillis();
			while (!isStopped.get()) {
				try {
					/*
					 * Wait for next cycle
					 */
					try {
						int cycleTime = getCycleTime();
						if (cycleTime == DO_NOT_WAIT) {
							// no wait
						} else if (cycleTime > 0) {
							long sleep = cycleTime - (System.currentTimeMillis() - cycleStart);
							if (sleep > 0) {
								Thread.sleep(sleep);
							}
						} else { // < 0 (ALWAYS_WAIT_FOR_TRIGGER_NEXT_RUN)
							AbstractWorker.this.cycleMutex.await();
						}
					} catch (InterruptedException e) {
						if (AbstractWorker.this.isTriggerRun.get()) {
							// check if a "forceRun" was triggered. In that case Thread.sleep is
							// interrupted and run() is
							// starting again immediately
							AbstractWorker.this.isTriggerRun.set(false);
						} else {
							// otherwise forward the exception
							AbstractWorker.this.isStopped.set(true);
							throw e;
						}
					}

					// store start time
					cycleStart = System.currentTimeMillis();

					// reset Force-Run in any case
					AbstractWorker.this.isTriggerRun.set(false);

					/*
					 * Call forever() forever.
					 */
					forever();

					// Everything went ok -> reset onWorkerExceptionSleep
					onWorkerExceptionSleep = 1;
				} catch (Throwable e) {
					/*
					 * Handle Worker-Exceptions
					 */
					log.error("Worker error. " + e.getClass().getSimpleName() + ": " + e.getMessage() //
							+ (e.getCause() != null ? " - Caused by: " + e.getCause().getMessage() : ""));
					e.printStackTrace();
					onWorkerExceptionSleep = onWorkerExceptionSleep(onWorkerExceptionSleep);
				}
			}
		};
	};

	/**
	 * Little helper method: Sleep and don't let yourself interrupt by a
	 * ForceRun-Flag. It is not making sense anyway, because something is wrong with
	 * the setup if we landed here.
	 *
	 * @param duration in seconds
	 */
	private long onWorkerExceptionSleep(long duration) {
		if (duration < 60) {
			duration += 1;
		}
		long targetTime = System.currentTimeMillis() + (duration * 1000);
		do {
			try {
				long thisDuration = (targetTime - System.currentTimeMillis()) / 1000;
				if (thisDuration > 0) {
					Thread.sleep(thisDuration);
				}
			} catch (InterruptedException e1) {
				log.warn("WorkerExceptionSleep caused " + e1.getMessage());
			}
		} while (targetTime > System.currentTimeMillis());
		return duration;
	}
}
