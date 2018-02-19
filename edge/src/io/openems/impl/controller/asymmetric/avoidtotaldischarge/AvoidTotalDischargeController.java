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
package io.openems.impl.controller.asymmetric.avoidtotaldischarge;

import java.time.LocalDate;
import java.time.Period;
import java.util.Optional;
import java.util.Set;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.thingstate.ThingStateChannels;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.impl.controller.asymmetric.avoidtotaldischarge.Ess.State;

@ThingInfo(title = "Avoid total discharge of battery (Asymmetric)", description = "Makes sure the battery is not going into critically low state of charge. For asymmetric Ess.")
public class AvoidTotalDischargeController extends Controller implements ChannelChangeListener {

	private ThingStateChannels thingState = new ThingStateChannels(this);
	/*
	 * Constructors
	 */
	public AvoidTotalDischargeController() {
		super();
	}

	public AvoidTotalDischargeController(String id) {
		super(id);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "Ess", description = "Sets the Ess devices.", type = Ess.class, isArray = true)
	public final ConfigChannel<Set<Ess>> esss = new ConfigChannel<Set<Ess>>("esss", this);
	@ChannelInfo(title = "Max Soc", description = "If the System is full the charge is blocked untill the soc decrease below the maxSoc.", type = Long.class, defaultValue = "95")
	public final ConfigChannel<Long> maxSoc = new ConfigChannel<Long>("maxSoc", this);
	@ChannelInfo(title = "Next Discharge", description = "Next Time, the ess will discharge completely.", type = String.class,defaultValue = "2018-03-09")
	public final ConfigChannel<Long> nextDischarge = new ConfigChannel<Long>("nextDischarge", this).addChangeListener(this);
	@ChannelInfo(title = "Discharge Period", description = "The Period of time between two Discharges.https://docs.oracle.com/javase/8/docs/api/java/time/Period.html#parse-java.lang.CharSequence-", type = String.class,defaultValue = "P4W")
	public final ConfigChannel<Long> dischargePeriod = new ConfigChannel<Long>("dischargePeriod", this).addChangeListener(this);
	@ChannelInfo(title = "Enable Discharge", description="This option allowes the system to discharge the ess according to the nextDischarge completely. This improves the soc calculation.", type=Boolean.class,defaultValue="true")
	public final ConfigChannel<Boolean> enableDischarge = new ConfigChannel<Boolean>("EnableDischarge",this);

	private LocalDate nextDischargeDate;
	private Period period;

	/*
	 * Methods
	 */
	@Override
	public void run() {
		try {
			for (Ess ess : esss.value()) {
				/*
				 * Calculate SetActivePower according to MinSoc
				 */
				ess.stateMachineState.setValue(ess.currentState.value());
				switch (ess.currentState) {
				case CHARGESOC:
					if (ess.soc.value() > ess.minSoc.value()) {
						ess.currentState = State.MINSOC;
					} else {
						try {
							Optional<Long> currentMinValueL1 = ess.setActivePowerL1.writeMin();
							if (currentMinValueL1.isPresent() && currentMinValueL1.get() < 0) {
								// Force Charge with minimum of MaxChargePower/5
								log.info("Force charge. Set ActivePowerL1=Max[" + currentMinValueL1.get() / 5 + "]");
								ess.setActivePowerL1.pushWriteMax(currentMinValueL1.get() / 5);
							} else {
								log.info("Avoid discharge. Set ActivePowerL1=Max[-1000 W]");
								ess.setActivePowerL1.pushWriteMax(-1000L);
							}
						} catch (WriteChannelException e) {
							log.error("Unable to set ActivePowerL1: " + e.getMessage());
						}
						try {
							Optional<Long> currentMinValueL2 = ess.setActivePowerL2.writeMin();
							if (currentMinValueL2.isPresent() && currentMinValueL2.get() < 0) {
								// Force Charge with minimum of MaxChargePower/5
								log.info("Force charge. Set ActivePowerL2=Max[" + currentMinValueL2.get() / 5 + "]");
								ess.setActivePowerL2.pushWriteMax(currentMinValueL2.get() / 5);
							} else {
								log.info("Avoid discharge. Set ActivePowerL2=Max[-1000 W]");
								ess.setActivePowerL2.pushWriteMax(-1000L);
							}
						} catch (WriteChannelException e) {
							log.error("Unable to set ActivePowerL2: " + e.getMessage());
						}
						try {
							Optional<Long> currentMinValueL3 = ess.setActivePowerL3.writeMin();
							if (currentMinValueL3.isPresent() && currentMinValueL3.get() < 0) {
								// Force Charge with minimum of MaxChargePower/5
								log.info("Force charge. Set ActivePowerL3=Max[" + currentMinValueL3.get() / 5 + "]");
								ess.setActivePowerL3.pushWriteMax(currentMinValueL3.get() / 5);
							} else {
								log.info("Avoid discharge. Set ActivePowerL3=Max[-1000 W]");
								ess.setActivePowerL3.pushWriteMax(-1000L);
							}
						} catch (WriteChannelException e) {
							log.error("Unable to set ActivePowerL3: " + e.getMessage());
						}
					}
					break;
				case MINSOC:
					if (ess.soc.value() < ess.chargeSoc.value()) {
						ess.currentState = State.CHARGESOC;
					} else if (ess.soc.value() >= ess.minSoc.value() + 5) {
						ess.currentState = State.NORMAL;
					}else if(nextDischargeDate != null && nextDischargeDate.equals(LocalDate.now()) && enableDischarge.valueOptional().isPresent() && enableDischarge.valueOptional().get()) {
						ess.currentState = State.EMPTY;
					} else {
						try {
							long maxPower = 0;
							if (!ess.setActivePowerL1.writeMax().isPresent()
									|| maxPower < ess.setActivePowerL1.writeMax().get()) {
								ess.setActivePowerL1.pushWriteMax(maxPower);
							}
							if (!ess.setActivePowerL2.writeMax().isPresent()
									|| maxPower < ess.setActivePowerL2.writeMax().get()) {
								ess.setActivePowerL2.pushWriteMax(maxPower);
							}
							if (!ess.setActivePowerL3.writeMax().isPresent()
									|| maxPower < ess.setActivePowerL3.writeMax().get()) {
								ess.setActivePowerL3.pushWriteMax(maxPower);
							}
						} catch (WriteChannelException e) {
							log.error(ess.id() + "Failed to set Max allowed power.", e);
						}
					}
					break;
				case NORMAL:
					if (ess.soc.value() <= ess.minSoc.value()) {
						ess.currentState = State.MINSOC;
					} else if (ess.soc.value() >= 99 && ess.allowedCharge.value() == 0
							&& ess.systemState.labelOptional().equals(Optional.of(EssNature.START))) {
						ess.currentState = State.FULL;
					}
					break;
				case FULL:
					try {
						ess.setActivePowerL1.pushWriteMin(0L);
					} catch (WriteChannelException e) {
						log.error("Unable to set ActivePowerL1: " + e.getMessage());
					}
					try {
						ess.setActivePowerL2.pushWriteMin(0L);
					} catch (WriteChannelException e) {
						log.error("Unable to set ActivePowerL2: " + e.getMessage());
					}
					try {
						ess.setActivePowerL3.pushWriteMin(0L);
					} catch (WriteChannelException e) {
						log.error("Unable to set ActivePowerL3: " + e.getMessage());
					}
					if (ess.soc.value() < maxSoc.value()) {
						ess.currentState = State.NORMAL;
					}
					break;
				case EMPTY:
					if(ess.allowedDischarge.value() == 0) {
						//Ess is Empty set Date and charge to minSoc
						addPeriod();
						ess.currentState = State.CHARGESOC;
					}
					break;
				}
			}
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if(this.nextDischarge.equals(channel)) {
			if(newValue.isPresent()) {
				nextDischargeDate = LocalDate.parse((String)newValue.get());
			}else {
				nextDischargeDate = null;
			}
		}else if(this.dischargePeriod.equals(channel)) {
			if(newValue.isPresent()) {
				this.period = Period.parse((String)newValue.get());
			}else {
				this.period = null;
			}
		}
		if(nextDischargeDate != null && nextDischargeDate.isBefore(LocalDate.now())) {
			addPeriod();
		}
	}

	private void addPeriod() {
		if(this.nextDischargeDate != null && this.period != null) {
			this.nextDischargeDate.plus(period);
			nextDischarge.updateValue(this.nextDischargeDate.toString(),true);
		}
	}

	@Override
	public ThingStateChannels getStateChannel() {
		return this.thingState;
	}

}
