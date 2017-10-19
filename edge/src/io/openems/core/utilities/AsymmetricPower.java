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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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

	public enum ReductionType {
		PERPHASE, PERSUM
	}

	private final ReadChannel<Long> allowedDischarge;
	private final ReadChannel<Long> allowedCharge;
	private final ReadChannel<Long> allowedApparent;
	@SuppressWarnings("unchecked")
	private final WriteChannel<Long>[] setActivePower = new WriteChannel[3];
	@SuppressWarnings("unchecked")
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
	 *
	 * @throws WriteChannelException
	 */
	public void reducePower(ReductionType reductionType) throws WriteChannelException {

		// variables for reducedPower
		long[] reducedActivePower = Arrays.copyOf(activePower, 3);
		long[] reducedReactivePower = Arrays.copyOf(reactivePower, 3);
		@SuppressWarnings("unchecked") List<Long>[] minActivePowerPhase = new List[] { new ArrayList<Long>(),
				new ArrayList<Long>(), new ArrayList<Long>() };
		@SuppressWarnings("unchecked") List<Long>[] maxActivePowerPhase = new List[] { new ArrayList<Long>(),
				new ArrayList<Long>(), new ArrayList<Long>() };
		@SuppressWarnings("unchecked") List<Long>[] minReactivePowerPhase = new List[] { new ArrayList<Long>(),
				new ArrayList<Long>(), new ArrayList<Long>() };
		@SuppressWarnings("unchecked") List<Long>[] maxReactivePowerPhase = new List[] { new ArrayList<Long>(),
				new ArrayList<Long>(), new ArrayList<Long>() };
		long activePowerPosSum = 0;
		long activePowerNegSum = 0;
		long activePowerSum = 0;
		long reactivePowerSum = 0;
		for (int i = 0; i < 3; i++) {
			if (activePower[i] > 0) {
				activePowerPosSum += activePower[i];
			} else {
				activePowerNegSum += activePower[i];
			}
			activePowerSum += activePower[i];
			reactivePowerSum += reactivePower[i];
		}
		try {
			for (int i = 0; i < 3; i++) {
				// Check if active power is already set
				if (setActivePower[i].getWriteValue().isPresent()) {
					this.activePower[i] = setActivePower[i].getWriteValue().get();
				}
				// Check if reactive power is already set
				if (setReactivePower[i].getWriteValue().isPresent()) {
					this.reactivePower[i] = setReactivePower[i].getWriteValue().get();
				}
				// set limits by allowed apparent
				double cosPhi = ControllerUtils.calculateCosPhi(this.activePower[i], this.reactivePower[i]);
				long activePower = ControllerUtils.calculateActivePowerFromApparentPower(allowedApparent.value() / 3,
						cosPhi);
				long reactivePower = ControllerUtils.calculateReactivePower(activePower, allowedApparent.value() / 3);
				maxReactivePowerPhase[i].add(reactivePower);
				minReactivePowerPhase[i].add(reactivePower * -1);
				maxActivePowerPhase[i].add(activePower);
				minActivePowerPhase[i].add(activePower * -1);
				if (setReactivePower[i].writeMax().isPresent()) {
					maxReactivePowerPhase[i].add(setReactivePower[i].writeMax().get());
				}
				if (setReactivePower[i].writeMin().isPresent()) {
					minReactivePowerPhase[i].add(setReactivePower[i].writeMin().get());
				}
				if (setActivePower[i].writeMax().isPresent()) {
					maxActivePowerPhase[i].add(setActivePower[i].writeMax().get());
				}
				if (setActivePower[i].writeMin().isPresent()) {
					minActivePowerPhase[i].add(setActivePower[i].writeMin().get());
				}
				if (this.activePower[i] < 0) {
					minActivePowerPhase[i].add(allowedCharge.value() / activePowerNegSum * this.activePower[i]);
				}
				if (this.activePower[i] > 0) {
					maxActivePowerPhase[i].add(allowedDischarge.value() / activePowerPosSum * this.activePower[i]);
				}
			}

			switch (reductionType) {
			case PERSUM: {
				Long[] minActivePowers = new Long[] { Collections.max(minActivePowerPhase[0]),
						Collections.max(minActivePowerPhase[1]), Collections.max(minActivePowerPhase[2]) };
				Long[] maxActivePowers = new Long[] { Collections.min(maxActivePowerPhase[0]),
						Collections.min(maxActivePowerPhase[1]), Collections.min(maxActivePowerPhase[2]) };
				Long[] minReactivePowers = new Long[] { Collections.max(minReactivePowerPhase[0]),
						Collections.max(minReactivePowerPhase[1]), Collections.max(minReactivePowerPhase[2]) };
				Long[] maxReactivePowers = new Long[] { Collections.min(maxReactivePowerPhase[0]),
						Collections.min(maxReactivePowerPhase[1]), Collections.min(maxReactivePowerPhase[2]) };
				for (int i = 0; i < 3; i++) {
					if (activePower[i] > maxActivePowers[i]) {
						setMinMaxValues(maxActivePowers[i], activePower[i], maxActivePowerPhase, minActivePowerPhase,
								activePowerSum, i);
					} else if (activePower[i] < minActivePowers[i]) {
						setMinMaxValues(minActivePowers[i], activePower[i], maxActivePowerPhase, minActivePowerPhase,
								activePowerSum, i);
					}
					if (reactivePower[i] > maxReactivePowers[i]) {
						setMinMaxValues(maxReactivePowers[i], reactivePower[i], maxReactivePowerPhase,
								minReactivePowerPhase, reactivePowerSum, i);
					} else if (reactivePower[i] < minReactivePowers[i]) {
						setMinMaxValues(minReactivePowers[i], reactivePower[i], maxReactivePowerPhase,
								minReactivePowerPhase, reactivePowerSum, i);
					}
				}
			}
			default:
			case PERPHASE:

				break;
			}
			// reduce to min/max values
			for (int i = 0; i < 3; i++) {
				long minReactivePower = Collections.max(minReactivePowerPhase[i]);
				long maxReactivePower = Collections.min(maxReactivePowerPhase[i]);
				long minActivePower = Collections.max(minActivePowerPhase[i]);
				long maxActivePower = Collections.min(maxActivePowerPhase[i]);
				if (activePower[i] > maxActivePower) {
					reducedActivePower[i] = maxActivePower;
				} else if (activePower[i] < minActivePower) {
					reducedActivePower[i] = minActivePower;
				}
				if (reactivePower[i] > maxReactivePower) {
					reducedReactivePower[i] = maxReactivePower;
				} else if (reactivePower[i] < minReactivePower) {
					reducedReactivePower[i] = minReactivePower;
				}
			}
		} catch (

		InvalidValueException e) {
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
	 *
	 * @throws WriteChannelException
	 */
	public void writePower(ReductionType reductionType) throws WriteChannelException {
		this.reducePower(reductionType);
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
		activePower[0] = 0;
		activePower[1] = 0;
		activePower[2] = 0;
		reactivePowerValid = false;
		reactivePower[0] = 0;
		reactivePower[1] = 0;
		reactivePower[2] = 0;
	}

	private void setMinMaxValues(long limit, long power, List<Long>[] maxLimits, List<Long>[] minLimits, Long powerSum,
			int phase) {
		long diff = limit - power;
		for (int i = 0; i < 3; i++) {
			long delta = Math.abs(diff);
			if (i != phase) {
				delta /= 2;
			}
			if (this.activePower[i] > powerSum) {
				maxLimits[i].add(this.activePower[i] - delta);
			} else {
				minLimits[i].add(this.activePower[i] + delta);
			}
		}
	}

}
