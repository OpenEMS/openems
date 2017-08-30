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
package io.openems.impl.protocol.keba;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.Device;
import io.openems.api.doc.ThingInfo;

@ThingInfo(title = "KEBA KeContact Bridge")
public class KebaBridge extends Bridge {

	public KebaBridge() {
		log.info("Constructor KebaBridge");
	}

	/*
	 * Config
	 */
	private ConfigChannel<Integer> port = new ConfigChannel<Integer>("port", this).defaultValue(9070);

	/*
	 * Fields
	 */
	private Logger log = LoggerFactory.getLogger(KebaBridge.class);
	protected volatile KebaDevice[] kebadevices = new KebaDevice[0];
	private AtomicBoolean isWriteTriggered = new AtomicBoolean(false);
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private ScheduledFuture<?> receivingJob = null;

	/*
	 * Methods
	 */
	@Override
	public String toString() {
		return "KebaBridge []";
	}

	@Override
	public void triggerWrite() {
		// set the Write-flag
		isWriteTriggered.set(true);
		// start "run()" again as fast as possible
		// triggerForceRun();
	}

	// @Override
	// protected void forever() {
	// log.info("KebaBridge... forever");
	// for (KebaDevice kebadevice : kebadevices) {
	// log.info("KebaBridge... forever: " + kebadevice.id());
	// // if Write-flag was set -> start writing for all Devices immediately
	// if (isWriteTriggered.get()) {
	// isWriteTriggered.set(false);
	// writeAllDevices();
	// }
	// // Poll update for all reports
	// try {
	// log.info("KebaBridge... update: " + kebadevice.id());
	// kebadevice.update();
	// } catch (InterruptedException e) {
	// log.warn("Updating KebaDevice [{}] was interrupted: {}", kebadevice.id(), e.getMessage());
	// }
	// }
	// }
	//
	// private void writeAllDevices() {
	// for (KebaDevice kebadevice : kebadevices) {
	// try {
	// kebadevice.write();
	// } catch (InterruptedException e) {
	// log.error("Error while writing to KebaDevice [" + kebadevice.id() + "]: " + e.getMessage());
	// }
	// }
	// }

	@Override
	protected void dispose() {
		if (this.receivingJob != null) {
			this.receivingJob.cancel(true);
			this.receivingJob = null;
		}
	}

	@Override
	protected boolean initialize() {
		/*
		 * Copy and cast devices to local kebadevices array
		 */
		if (devices.isEmpty()) {
			return false;
		}
		List<KebaDevice> kebadevices = new ArrayList<>();
		for (Device device : devices) {
			if (device instanceof KebaDevice) {
				kebadevices.add((KebaDevice) device);
			}
		}
		KebaDevice[] newKebadevices = kebadevices.stream().toArray(KebaDevice[]::new);
		if (newKebadevices == null) {
			newKebadevices = new KebaDevice[0];
		}
		this.kebadevices = newKebadevices;
		/*
		 * Restart ReceivingRunnable
		 */
		// dispose();
		// Runnable receivingRunnable;
		// try {
		// receivingRunnable = new ReceivingRunnable(this.port.value());
		// } catch (InvalidValueException e) {
		// log.error("Error initializing KebaBridge: {}", e.getMessage());
		// return false;
		// }
		// this.receivingJob = scheduler.schedule(receivingRunnable, 0, TimeUnit.MILLISECONDS);
		return true;
	}
}
