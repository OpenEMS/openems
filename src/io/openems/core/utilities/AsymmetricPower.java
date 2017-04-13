/*******************************************************************************
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH and contributors
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

import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

/**
 * Helper class to reduce and set power to the ess
 *
 * @author matthias.rossmann
 *
 */
public class AsymmetricPower {
	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	private final ReadChannel<Long> allowedDischarge;
	private final ReadChannel<Long> allowedCharge;
	private final ReadChannel<Long> allowedApparent;
	private final WriteChannel<Long>[] setActivePower = new WriteChannel[3];
	private final WriteChannel<Long>[] setReactivePower = new WriteChannel[3];
	private long[] activePower = new long[3];
	private long[] reactivePower = new long[3];
	private boolean activePowerValid = false;
	private boolean reactivePowerValid = false;

	public AsymmetricPower(ReadChannel<Long> allowedDischarge, ReadChannel<Long> allowedCharge,
			ReadChannel<Long> allowedApparent, WriteChannel<Long> setActivePowerL1, WriteChannel<Long> setActivePowerL2,
			WriteChannel<Long> setActivePowerL3, WriteChannel<Long> setReactivePowerL1,
			WriteChannel<Long> setReactivePowerL2, WriteChannel<Long> setReactivePowerL3) {
		super();
		this.allowedDischarge = allowedDischarge;
		this.allowedCharge = allowedCharge;
		this.allowedApparent = allowedApparent;
		this.setActivePower[0] = setActivePowerL1;
		this.setActivePower[1] = setActivePowerL2;
		this.setActivePower[2] = setActivePowerL3;
		this.setReactivePower[0] = setReactivePowerL1;
		this.setReactivePower[1] = setReactivePowerL2;
		this.setReactivePower[2] = setReactivePowerL3;
	}

	public void setActivePower(long powerL1, long powerL2, long powerL3) {
		this.activePower[0] = powerL1;
		this.activePower[1] = powerL2;
		this.activePower[2] = powerL3;
		this.activePowerValid = true;
	}

	public void setReactivePower(long powerL1, long powerL2, long powerL3) {
		this.reactivePower[0] = powerL1;
		this.reactivePower[1] = powerL2;
		this.reactivePower[2] = powerL3;
		this.reactivePowerValid = true;
	}

	public long getActivePowerL1() {
		return this.activePower[0];
	}

	public long getReactivePowerL1() {
		return this.reactivePower[0];
	}

	public long getActivePowerL2() {
		return this.activePower[1];
	}

	public long getReactivePowerL2() {
		return this.reactivePower[1];
	}

	public long getActivePowerL3() {
		return this.activePower[2];
	}

	public long getReactivePowerL3() {
		return this.reactivePower[2];
	}

	/**
	 * Reduces the active and reactive power to the power limitations
	 */
	public void reducePower() {

		// variables for reducedPower
		long[] reducedActivePower = Arrays.copyOf(activePower, 3);
		long[] reducedReactivePower = Arrays.copyOf(reactivePower, 3);

		try {
			// Check if active power is already set
			for (int i = 0; i < 3; i++) {
				if (setActivePower[i].getWriteValue().isPresent()) {
					this.activePower[i] = setActivePower[i].getWriteValue().get();
					try {
						this.setReactivePower[i].pushWriteMax(ControllerUtils
								.calculateReactivePower(this.activePower[i], allowedApparent.value() / 3));
					} catch (WriteChannelException e) {}
					try {
						this.setReactivePower[i].pushWriteMin(
								ControllerUtils.calculateReactivePower(this.activePower[i], allowedApparent.value() / 3)
										* -1);
					} catch (WriteChannelException e) {}
				}
				// Check if reactive power is already set
				if (setReactivePower[i].getWriteValue().isPresent()) {
					this.reactivePower[i] = setReactivePower[i].getWriteValue().get();
					try {
						this.setActivePower[i].pushWriteMax(ControllerUtils.calculateActivePower(this.reactivePower[i],
								allowedApparent.value() / 3));
					} catch (WriteChannelException e) {}
					try {
						this.setActivePower[i].pushWriteMin(
								ControllerUtils.calculateActivePower(this.reactivePower[i], allowedApparent.value() / 3)
										* -1);
					} catch (WriteChannelException e) {}
				}
				// Check if ReactivePower is in allowed Range
				if (Math.abs(reactivePower[i]) > allowedApparent.value() / 3) {
					if (reactivePower[i] > 0) {
						reducedReactivePower[i] = allowedApparent.value() / 3;
					} else {
						reducedReactivePower[i] = allowedApparent.value() / -3;
					}
				}
				if (setReactivePower[i].writeMax().isPresent()
						&& reactivePower[i] > setReactivePower[i].writeMax().get()) {
					reducedReactivePower[i] = setReactivePower[i].writeMax().get();
				}
				if (setReactivePower[i].writeMin().isPresent()
						&& reactivePower[i] < setReactivePower[i].writeMin().get()) {
					reducedReactivePower[i] = setReactivePower[i].writeMin().get();
				}
				try {
					this.setActivePower[i].pushWriteMax(
							ControllerUtils.calculateActivePower(reducedReactivePower[i], allowedApparent.value() / 3));
				} catch (WriteChannelException e) {}
				try {
					this.setActivePower[i].pushWriteMin(
							ControllerUtils.calculateActivePower(reducedReactivePower[i], allowedApparent.value() / 3)
									* -1);
				} catch (WriteChannelException e) {}
				// Reduce ActivePower to min/max value
				if (setActivePower[i].writeMax().isPresent() && activePower[i] > setActivePower[i].writeMax().get()) {
					reducedActivePower[i] = setActivePower[i].writeMax().get();
				}
				if (setActivePower[i].writeMin().isPresent() && activePower[i] < setActivePower[i].writeMin().get()) {
					reducedActivePower[i] = setActivePower[i].writeMin().get();
				}
			}
			// Reduce ActivePower to allowedCharge/Discharge
			long activePowerSum = reducedActivePower[0] + reducedActivePower[1] + reducedActivePower[2];
			long delta = 0L;
			if (activePowerSum < allowedCharge.value()) {
				delta = (activePowerSum - allowedCharge.value()) / 3;
			} else if (activePowerSum > allowedDischarge.value()) {
				delta = (activePowerSum - allowedDischarge.value()) / 3;
			}
			for (int i = 0; i < 3; i++) {
				reducedActivePower[i] -= delta;
			}
		} catch (InvalidValueException e) {
			log.error("Failed to reduce power", e);
		}
		log.info(
				"Reduce activePower L1:[{}]->[{}], L2:[{}]->[{}],L3:[{}]->[{}] "
						+ "and reactivePower L1:[{}]->[{}], L2:[{}]->[{}], L3:[{}]->[{}]",
				new Object[] { activePower[0], reducedActivePower[0], activePower[1], reducedActivePower[1],
						activePower[2], reducedActivePower[2], reactivePower[0], reducedReactivePower[0],
						reactivePower[1], reducedReactivePower[1], reactivePower[2], reducedReactivePower[2] });
		for (int i = 0; i < 3; i++) {
			this.activePower[i] = reducedActivePower[i];
			this.reactivePower[i] = reducedReactivePower[i];
		}
	}

	/**
	 * Writes active and reactive power to the setActive-/setReactivePower Channel if the value was set
	 */
	public void writePower() {
		this.reducePower();
		try {
			for (int i = 0; i < 3; i++) {
				if (activePowerValid) {
					setActivePower[i].pushWrite(activePower[i]);
				}
				if (reactivePowerValid) {
					setReactivePower[i].pushWrite(reactivePower[i]);
				}
			}
		} catch (WriteChannelException e1) {
			log.error("Failed to reduce and set Power!", e1);
		}
		activePowerValid = false;
		reactivePowerValid = false;
	}

}
