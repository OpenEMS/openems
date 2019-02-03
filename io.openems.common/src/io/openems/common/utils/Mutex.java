package io.openems.common.utils;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Mutex {

	private final Semaphore semaphore;

	public Mutex(boolean initiallyPermitted) {
		if (initiallyPermitted) {
			semaphore = new Semaphore(1);
		} else {
			semaphore = new Semaphore(0);
		}
	}

	public void await() throws InterruptedException {
		int permits = semaphore.drainPermits();
		if (permits == 0) {
			semaphore.acquire();
		}
	}

	public void awaitOrTimeout(long timeout, TimeUnit unit) throws InterruptedException {
		int permits = semaphore.drainPermits();
		if (permits == 0) {
			semaphore.tryAcquire(timeout, unit);
		}
	}

	public void release() {
		semaphore.release();
	}
}
