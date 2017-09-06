/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016, 2017 FENECON GmbH and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * Contributors:
 *   FENECON GmbH - initial API and implementation and initial documentation
 *******************************************************************************/
package io.openems.core.utilities;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.thing.Thing;

public abstract class AbstractWorker extends Thread implements Thing {
	private final AtomicBoolean initialize = new AtomicBoolean(true);
	private Mutex initializedMutex = new Mutex(false);
	private final AtomicBoolean isForceRun = new AtomicBoolean(false);
	private final AtomicBoolean isInitialized = new AtomicBoolean(false);
	private final AtomicBoolean isStopped = new AtomicBoolean(false);
	private long cycleStart = 0;
	protected final Logger log;

	/**
	 * Initialize the Thread with a name
	 *
	 * @param name
	 */
	public AbstractWorker(String name) {
		log = LoggerFactory.getLogger(this.getClass());
		this.setName(name);
	}

	// @ConfigInfo(title = "Sets the duration of each cycle in milliseconds", type = Integer.class)
	// public abstract ConfigChannel<Integer> cycleTime();

	protected abstract int getCycleTime();

	public boolean isInitialized() {
		return isInitialized.get();
	}

	/**
	 * Little helper method: Sleep and don't let yourself interrupt by a ForceRun-Flag. It is not making sense anyway,
	 * because something is wrong with the setup if we landed here.
	 *
	 * @param duration
	 *            in seconds
	 */
	private long bridgeExceptionSleep(long duration) {
		if (duration < 60) {
			duration += 1;
		}
		long targetTime = System.nanoTime() + (duration * 1000000);
		do {
			try {
				long thisDuration = (targetTime - System.nanoTime()) / 1000000;
				if (thisDuration > 0) {
					Thread.sleep(thisDuration);
				}
			} catch (InterruptedException e1) {}
		} while (targetTime > System.nanoTime());
		return duration;
	}

	/**
	 * This method is called when the Thread stops. Use it to close resources.
	 */
	protected abstract void dispose();

	/**
	 * This method is called in a loop forever until the Thread gets interrupted.
	 */
	protected abstract void forever();

	@Override
	public String id() {
		return getName();
	};

	/**
	 * This method is called once before {@link forever()} and every time after {@link restart()} method was called. Use
	 * it to (re)initialize everything.
	 *
	 * @return false on initialization error
	 */
	protected abstract boolean initialize();

	@Override
	public void interrupt() {
		// TODO Auto-generated method stub
		super.interrupt();
	}

	/**
	 * Executes the Thread. Calls {@link forever} till the Thread gets interrupted.
	 */
	@Override
	public final void run() {
		long bridgeExceptionSleep = 1; // seconds
		this.initialize.set(true);
		while (!isStopped.get()) {
			cycleStart = System.currentTimeMillis();
			try {
				/*
				 * Initialize Bridge
				 */
				while (initialize.get()) {
					boolean initSuccessful = initialize();
					if (initSuccessful) {
						isInitialized.set(true);
						initializedMutex.release();
						initialize.set(false);
					} else {
						initializedMutex.awaitOrTimeout(getCycleTime() * 10, TimeUnit.MILLISECONDS);
					}
				}
				/*
				 * Call forever() forever.
				 */
				forever();
				/*
				 * Wait for next cycle
				 */
				try {
					long sleep = getCycleTime() - (System.currentTimeMillis() - cycleStart);
					if (sleep > 0) {
						Thread.sleep(sleep); // TODO add cycle time
					}
				} catch (InterruptedException e) {
					if (isForceRun.get()) {
						// check if a "forceRun" was triggereed. In that case Thread.sleep is interrupted and run() is
						// starting again immediately
						isForceRun.set(false);
					} else {
						// otherwise forward the exception
						isStopped.set(true);
						throw e;
					}
				}
				// Everything went ok: reset bridgeExceptionSleep
				bridgeExceptionSleep = 1;
			} catch (Throwable e) {
				/*
				 * Handle Bridge-Exceptions
				 */
				log.error("Bridge-Exception! Retry later: ", e);
				bridgeExceptionSleep = bridgeExceptionSleep(bridgeExceptionSleep);
			}
		}
		dispose();
		System.out.println("BridgeWorker was interrupted. Exiting gracefully...");
	}

	/**
	 * Causes the Worker to interrupt sleeping and start again the run() method immediately
	 */
	public final void triggerForceRun() {
		if (!isForceRun.getAndSet(true)) {
			this.interrupt();
		}
	}

	public void shutdown() {
		isStopped.set(true);
	}

	/**
	 * Triggers a call of {@link initialization()} in the next loop. Call this, if the configuration changes.
	 */
	public final void triggerInitialize() {
		initialize.set(true);
		initializedMutex.release();
	}
}
