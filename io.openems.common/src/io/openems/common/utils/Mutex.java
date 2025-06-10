package io.openems.common.utils;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Mutex {

	private final Semaphore semaphore;

	public Mutex(boolean initiallyPermitted) {
		if (initiallyPermitted) {
			this.semaphore = new Semaphore(1);
		} else {
			this.semaphore = new Semaphore(0);
		}
	}

	/**
	 * Wait for a {@link #release()}.
	 *
	 * @throws InterruptedException on wait error
	 */
	public void await() throws InterruptedException {
		var permits = this.semaphore.drainPermits();
		if (permits == 0) {
			this.semaphore.acquire();
		}
	}

	/**
	 * Wait for a {@link #release()} with a timeout.
	 *
	 * @param timeout the timeout value
	 * @param unit    the timeout {@link TimeUnit}
	 * @throws InterruptedException on wait error
	 */
	public void awaitOrTimeout(long timeout, TimeUnit unit) throws InterruptedException {
		var permits = this.semaphore.drainPermits();
		if (permits == 0) {
			this.semaphore.tryAcquire(timeout, unit);
		}
	}

	/**
	 * Release the {@link Mutex}.
	 */
	public void release() {
		this.semaphore.release();
	}
}
