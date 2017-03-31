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
package io.openems.impl.controller.symmetric.refuavoidtotaldischarge;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.core.utilities.hysteresis.Hysteresis;

public class AvoidTotalDischargeController extends Controller {

	@ConfigInfo(title = "Storage, where total discharge should be avoided. For excample to reserve load for the Off-Grid power supply.", type = Ess.class)
	public final ConfigChannel<Ess> ess = new ConfigChannel<Ess>("ess", this);
	@ConfigInfo(title = "Delay, to allow Power after start", type = Long.class)
	public final ConfigChannel<Long> powerDelay = new ConfigChannel<>("powerDelay", this);
	@ConfigInfo(title = "Step to increase the allowed power after start delay.", type = Long.class)
	public final ConfigChannel<Long> powerStep = new ConfigChannel<>("powerStep", this);
	@ConfigInfo(title = "Soc limit to stop chargePower.", type = Long.class)
	public final ConfigChannel<Long> maxSoc = new ConfigChannel<Long>("maxSoc", this)
			.addChangeListener(new ChannelChangeListener() {

				@Override
				public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
					if (maxSoc.valueOptional().isPresent() && socHysteresis.valueOptional().isPresent()) {
						try {
							socMaxHysteresis = new Hysteresis(maxSoc.value() - socHysteresis.value(), maxSoc.value());
						} catch (InvalidValueException e) {}
					}
				}
			});
	@ConfigInfo(title = "Soc hysteresis for max Soc limit.", type = Long.class)
	public final ConfigChannel<Long> socHysteresis = new ConfigChannel<Long>("socHysteresis", this)
			.addChangeListener(new ChannelChangeListener() {

				@Override
				public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
					if (maxSoc.valueOptional().isPresent() && socHysteresis.valueOptional().isPresent()) {
						try {
							socMaxHysteresis = new Hysteresis(maxSoc.value() - socHysteresis.value(), maxSoc.value());
						} catch (InvalidValueException e) {}
					}
				}
			});
	private boolean isStart = false;
	private long timeStartOccured = System.currentTimeMillis();
	private long lastPower = 0L;
	private Hysteresis socMaxHysteresis;

	public AvoidTotalDischargeController() {
		super();
	}

	public AvoidTotalDischargeController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();

			if (ess.systemState.value() == 4 ) {
				if (!isStart) {
					timeStartOccured = System.currentTimeMillis();
					isStart = true;
				}
			} else {
				isStart = false;
				lastPower = 0L;
			}
			if (isStart && timeStartOccured + powerDelay.value() <= System.currentTimeMillis()) {
				lastPower += powerStep.value();
				if (lastPower > ess.nominalPower.value()) {
					lastPower = ess.nominalPower.value();
				}
			}
			try {
				ess.setActivePower.pushWriteMin(lastPower * -1);
			} catch (WriteChannelException e) {
				// catch out of bounds
			}
			try {
				ess.setActivePower.pushWriteMax(lastPower);
			} catch (WriteChannelException e) {
				// catch out of bounds
			}
			try {
				ess.setReactivePower.pushWriteMin(lastPower * -1);
			} catch (WriteChannelException e) {
				// catch out of bounds
			}
			try {
				ess.setReactivePower.pushWriteMax(lastPower);
			} catch (WriteChannelException e) {
				// catch out of bounds
			}

			/*
			 * Calculate SetActivePower according to MinSoc
			 */

			ess.socMinHysteresis.apply(ess.soc.value(), (state, multiplier) -> {
				switch (state) {
				case ASC:
				case DESC:
					if (!ess.isChargeSoc) {
						try {
							long maxPower = 0;
							if (!ess.setActivePower.writeMax().isPresent()
									|| maxPower < ess.setActivePower.writeMax().get()) {
								ess.setActivePower.pushWriteMax(maxPower);
							}
						} catch (WriteChannelException e) {
							log.error(ess.id() + "Failed to set Max allowed power.", e);
						}
					}
					break;
				case BELOW:
					if (!ess.isChargeSoc) {
						try {
							if (ess.soc.value() < ess.chargeSoc.value()) {
								ess.isChargeSoc = true;
							}
						} catch (Exception e) {
							log.error(e.getMessage());
						}
					}
					break;
				case ABOVE:
					ess.isChargeSoc = false;
				default:
					break;
				}
				if (ess.isChargeSoc) {
					try {
						Optional<Long> currentMinValue = ess.setActivePower.writeMin();
						if (currentMinValue.isPresent() && currentMinValue.get() < 0) {
							// Force Charge with minimum of MaxChargePower/5
							log.info("Force charge. Set ActivePower=Max[" + currentMinValue.get() / 5 + "]");
							ess.setActivePower.pushWriteMax(currentMinValue.get() / 5);
						} else {
							log.info("Avoid discharge. Set ActivePower=Max[-1000 W]");
							ess.setActivePower.pushWriteMax(-1000L);
						}
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			});

			if (socMaxHysteresis != null) {
				socMaxHysteresis.apply(ess.soc.value(), (state, multiplier) -> {
					switch (state) {
					case ABOVE:
						try {
							ess.setActivePower.pushWriteMin(0L);
						} catch (WriteChannelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					case ASC:
						try {
							ess.setActivePower.pushWriteMin((long) (ess.allowedCharge.value() * multiplier));
						} catch (WriteChannelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (InvalidValueException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					case BELOW:
						break;
					case DESC:
						try {
							ess.setActivePower.pushWriteMin(0L);
						} catch (WriteChannelException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						break;
					default:
						break;
					}
				});
			}
		} catch (InvalidValueException e) {
			log.error(e.getMessage());
		}
	}
}
