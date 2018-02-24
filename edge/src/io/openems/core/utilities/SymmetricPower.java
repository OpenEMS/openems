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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.exception.WriteChannelException;

/**
 * Helper class to reduce and set power to the ess
 *
 * @author matthias.rossmann
 *
 */
public class SymmetricPower {
	protected final Logger log = LoggerFactory.getLogger(this.getClass());

	private final ReadChannel<Long> allowedDischarge;
	private final ReadChannel<Long> allowedCharge;
	private final ReadChannel<Long> allowedApparent;
	private final WriteChannel<Long> setActivePower;
	private final WriteChannel<Long> setReactivePower;
	private long activePower = 0L;
	private long reactivePower = 0L;
	private boolean activePowerValid = false;
	private boolean reactivePowerValid = false;

	public SymmetricPower(ReadChannel<Long> allowedDischarge, ReadChannel<Long> allowedCharge,
			ReadChannel<Long> allowedApparent, WriteChannel<Long> setActivePower, WriteChannel<Long> setReactivePower) {
		super();
		this.allowedDischarge = allowedDischarge;
		this.allowedCharge = allowedCharge;
		this.allowedApparent = allowedApparent;
		this.setActivePower = setActivePower;
		this.setReactivePower = setReactivePower;
	}

	public void setActivePower(long power) {
		this.activePower = power;
		this.activePowerValid = true;
	}

	public void setReactivePower(long power) {
		this.reactivePower = power;
		this.reactivePowerValid = true;
	}

	public long getActivePower() {
		return this.activePower;
	}

	public long getReactivePower() {
		return this.reactivePower;
	}

	/**
	 * Reduces the active and reactive power to the power limitations
	 */
	public void reducePower() {
		// get Min/Max values
		long minActivePower = getMinActivePower();
		long maxActivePower = getMaxActivePower();
		long minReactivePower = getMinReactivePower();
		long maxReactivePower = getMaxReactivePower();

		// variables for reducedPower
		long reducedActivePower = 0L;
		long reducedReactivePower = 0L;

		// Check if active power is already set
		if (setActivePower.getWriteValue().isPresent()) {
			this.activePower = setActivePower.peekWrite().get();
		}
		// Check if reactive power is already set
		if (setReactivePower.getWriteValue().isPresent()) {
			this.reactivePower = setReactivePower.peekWrite().get();
		}

		// calculate cosPhi
		double cosPhi = ControllerUtils.calculateCosPhi(activePower, reactivePower);

		if (minActivePower <= activePower && activePower <= maxActivePower && minReactivePower <= reactivePower
				&& reactivePower <= maxReactivePower) {
			// activePower and reactivePower are in allowed value range
			// no need to reduce power
			reducedActivePower = activePower;
			reducedReactivePower = reactivePower;
		} else if ((minActivePower > activePower || activePower > maxActivePower)
				&& (minReactivePower > reactivePower || reactivePower > maxReactivePower)) {
			// activePower and reactivePower are out of allowed value range
			long reducedActivePower1 = 0L;
			long reducedActivePower2 = 0L;
			long reducedReactivePower1 = 0L;
			long reducedReactivePower2 = 0L;
			if (!ControllerUtils.isCharge(activePower, reactivePower)) {
				// Discharge
				reducedActivePower1 = minActivePower;
				reducedReactivePower1 = ControllerUtils.calculateReactivePower(reducedActivePower, cosPhi);
				reducedReactivePower2 = minReactivePower;
				reducedActivePower2 = ControllerUtils.calculateActivePowerFromReactivePower(reducedReactivePower2,
						cosPhi);
			} else {
				// Charge
				reducedActivePower1 = maxActivePower;
				reducedReactivePower1 = ControllerUtils.calculateReactivePower(reducedActivePower, cosPhi);
				reducedReactivePower2 = maxReactivePower;
				reducedActivePower2 = ControllerUtils.calculateActivePowerFromReactivePower(reducedReactivePower2,
						cosPhi);
			}
			// get largest fitting active and reactive power for min max values
			if (ControllerUtils.calculateApparentPower(reducedActivePower1, reducedReactivePower1) > ControllerUtils
					.calculateApparentPower(reducedActivePower2, reducedReactivePower2)
					&& minActivePower <= reducedActivePower1 && reducedActivePower1 <= maxActivePower
					&& minReactivePower <= reducedReactivePower1 && reducedReactivePower1 <= maxReactivePower) {
				reducedActivePower = reducedActivePower1;
				reducedReactivePower = reducedReactivePower1;
			} else if (minActivePower <= reducedActivePower2 && reducedActivePower2 <= maxActivePower
					&& minReactivePower <= reducedReactivePower2 && reducedReactivePower2 <= maxReactivePower) {
				reducedActivePower = reducedActivePower2;
				reducedReactivePower = reducedReactivePower2;
			} else {
				log.error("Can't reduce power to fit the power limitations!");
			}
		} else if (minActivePower > activePower || activePower > maxActivePower) {
			// only activePower is out of allowed value range
			if (minActivePower > activePower) {
				// Discharge
				reducedActivePower = minActivePower;
				reducedReactivePower = ControllerUtils.calculateReactivePower(reducedActivePower, cosPhi);
			} else {
				// Charge
				reducedActivePower = maxActivePower;
				reducedReactivePower = ControllerUtils.calculateReactivePower(reducedActivePower, cosPhi);
			}
		} else {
			// only reactivePower is out of allowed value range
			if (minReactivePower > reactivePower) {
				// Discharge
				reducedReactivePower = minReactivePower;
				reducedActivePower = ControllerUtils.calculateActivePowerFromReactivePower(reducedReactivePower,
						cosPhi);
			} else {
				// Charge
				reducedReactivePower = maxReactivePower;
				reducedActivePower = ControllerUtils.calculateActivePowerFromReactivePower(reducedReactivePower,
						cosPhi);
			}
		}
		if (activePower != reducedActivePower || reactivePower != reducedReactivePower) {
			log.info("Reduce activePower from [{}] to [{}] and reactivePower from [{}] to [{}]",
					new Object[] { activePower, reducedActivePower, reactivePower, reducedReactivePower });
		}
		this.activePower = reducedActivePower;
		this.reactivePower = reducedReactivePower;
	}

	/**
	 * Writes active and reactive power to the setActive-/setReactivePower Channel if the value was set
	 */
	public void writePower() {
		this.reducePower();
		try {
			// activePowerQueue.add(activePower);
			if (activePowerValid) {
				setActivePower.pushWrite(activePower);
			}
			// reactivePowerQueue.add(reactivePower);
			if (reactivePowerValid) {
				setReactivePower.pushWrite(reactivePower);
			}
		} catch (WriteChannelException e) {
			log.error("Failed to reduce and set Power!");
		}
		activePowerValid = false;
		reactivePowerValid = false;
		activePower = 0L;
		reactivePower = 0L;
	}

	/**
	 * Get the max active power out of allowedDischarge, allowedApparent and writeMax of setActivePower channels
	 *
	 * @return max allowed activePower
	 */
	public long getMaxActivePower() {
		long maxPower = 0;
		boolean valid = false;
		if (allowedDischarge.valueOptional().isPresent()) {
			maxPower = allowedDischarge.valueOptional().get();
			valid = true;
		}
		if (valid && allowedApparent.valueOptional().isPresent()) {
			maxPower = Math.min(maxPower, allowedApparent.valueOptional().get());
		} else if (allowedApparent.valueOptional().isPresent()) {
			maxPower = allowedApparent.valueOptional().get();
			valid = true;
		}
		if (valid && setActivePower.writeMax().isPresent()) {
			maxPower = Math.min(maxPower, setActivePower.writeMax().get());
		} else if (setActivePower.writeMax().isPresent()) {
			maxPower = setActivePower.writeMax().get();
		}
		if (!valid) {
			log.error("Failed to get Max value for ActivePower! Return 0.");
		}
		return maxPower;
	}

	/**
	 * Get the min active power out of allowedCharge, allowedApparent and writeMin of setActivePower channels
	 *
	 * @return min allowed activePower
	 */
	public long getMinActivePower() {
		long minPower = 0;
		boolean valid = false;
		if (allowedCharge.valueOptional().isPresent()) {
			minPower = allowedCharge.valueOptional().get();
			valid = true;
		}
		if (valid && allowedApparent.valueOptional().isPresent()) {
			minPower = Math.max(minPower, allowedApparent.valueOptional().get() * -1);
		} else if (allowedApparent.valueOptional().isPresent()) {
			minPower = allowedApparent.valueOptional().get() * -1;
			valid = true;
		}
		if (valid && setActivePower.writeMin().isPresent()) {
			minPower = Math.max(minPower, setActivePower.writeMin().get());
		} else if (setActivePower.writeMin().isPresent()) {
			minPower = setActivePower.writeMin().get();
		}
		if (!valid) {
			log.error("Failed to get Min value for ActivePower! Return 0.");
		}
		return minPower;
	}

	/**
	 * Get the max reactive power out of allowedDischarge, allowedApparent and writeMax of setReactivePower channels
	 *
	 * @return max allowed reactivePower
	 */
	public long getMaxReactivePower() {
		long maxPower = 0;
		boolean valid = false;
		if (allowedApparent.valueOptional().isPresent()) {
			maxPower = allowedApparent.valueOptional().get();
			valid = true;
		}
		if (valid && setReactivePower.writeMax().isPresent()) {
			maxPower = Math.min(maxPower, setReactivePower.writeMax().get());
		} else if (setReactivePower.writeMax().isPresent()) {
			maxPower = setReactivePower.writeMax().get();
		}
		if (!valid) {
			log.debug("Failed to get Max value for ReactivePower! Return 0.");
		}
		return maxPower;
	}

	/**
	 * Get the min reactive power out of allowedCharge, allowedApparent and writeMin of setReactivePower channels
	 *
	 * @return min allowed reactivePower
	 */
	public long getMinReactivePower() {
		long minPower = 0;
		boolean valid = false;
		if (allowedApparent.valueOptional().isPresent()) {
			minPower = allowedApparent.valueOptional().get() * -1;
			valid = true;
		}
		if (valid && setReactivePower.writeMin().isPresent()) {
			minPower = Math.max(minPower, setReactivePower.writeMin().get());
		} else if (setReactivePower.writeMin().isPresent()) {
			minPower = setReactivePower.writeMin().get();
		}
		if (!valid) {
			log.debug("Failed to get Min value for ReactivePower! Return 0.");
		}
		return minPower;
	}

}
