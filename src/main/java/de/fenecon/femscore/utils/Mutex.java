package de.fenecon.femscore.utils;

import java.util.concurrent.Semaphore;

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

	public void release() {
		semaphore.release();
	}
}
