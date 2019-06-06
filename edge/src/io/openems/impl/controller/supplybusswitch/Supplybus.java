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
package io.openems.impl.controller.supplybusswitch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.WriteChannel;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

public class Supplybus {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	private HashMap<Ess, WriteChannel<Boolean>> switchEssMapping;
	private WriteChannel<Long> supplybusOnIndication;
	private List<WriteChannel<Long>> loads;
	private Boolean[] loadState;
	private Ess primaryEss;
	private int loadIndex = 0;
	private long timeLoadSwitched = 0L;

	private Ess activeEss;

	private long lastTimeDisconnected;

	private long switchDelay;

	enum State {
		CONNECTED, DISCONNECTING, DISCONNECTED, CONNECTING, UNKNOWN
	}

	private State state;

	private String name;

	public Supplybus(HashMap<Ess, WriteChannel<Boolean>> switchEssMapping, String name, Ess primaryEss,
			long switchDelay, WriteChannel<Long> supplybusOnIndication, List<WriteChannel<Long>> loads) {
		this.switchEssMapping = switchEssMapping;
		this.name = name;
		this.primaryEss = primaryEss;
		this.switchDelay = switchDelay;
		this.supplybusOnIndication = supplybusOnIndication;
		state = State.UNKNOWN;
		this.loads = loads;
		this.loadState = new Boolean[loads.size()];
	}

	public void setSwitchDelay(long delay) {
		this.switchDelay = delay;
	}

	public String getName() {
		return name;
	}

	public void run() throws InvalidValueException {
		// WriteLoad values
		for (int i = 0; i < loads.size(); i++) {
			if (loadState[i] != null && loadState[i] == false) {
				try {
					loads.get(i).pushWrite(0L);
				} catch (WriteChannelException e) {
					logWarn("Failed to set loadState: " + e.getMessage());
				}
			}
		}
		StringBuilder sb = new StringBuilder(state.toString());
		if (activeEss != null) {
			sb.append(" ");
			sb.append(activeEss.id());
		}
		logInfo(sb.toString());
		switch (state) {
		case CONNECTED: {
			Ess active;
			try {
				active = getActiveEss();
				if (active == null || active != this.activeEss) {
					state = State.DISCONNECTING;
				} else {
					// check if ess is empty
					if ((active.gridMode.labelOptional().equals(Optional.of(EssNature.OFF_GRID))
							&& active.soc.value() < active.minSoc.value())
							|| active.systemState.labelOptional().equals(Optional.of(EssNature.FAULT))
							|| active.systemState.labelOptional().equals(Optional.of(EssNature.STOP))) {
						state = State.DISCONNECTING;
					} else {
						if (supplybusOnIndication != null) {
							try {
								supplybusOnIndication.pushWrite(1L);
							} catch (WriteChannelException e) {
								logError("can't set supplybusOnIndication: " + e.getMessage());
							}
						}
					}
				}
			} catch (SupplyBusException e1) {
				state = State.DISCONNECTING;
			}
		}
		break;
		case CONNECTING: {
			// if not connected send connect command again
			if (isConnected()) {
				if (supplybusOnIndication != null) {
					try {
						supplybusOnIndication.pushWrite(1L);
					} catch (WriteChannelException e) {
						logError("can't set supplybusOnIndication: " + e.getMessage());
					}
				}
				// connect all loads after ess connected and started
				try {
					if (connectLoads()) {
						state = State.CONNECTED;
					}
				} catch (WriteChannelException e) {
					logWarn("Can't start load: " + e.getMessage());
				}
			} else {
				if (lastTimeDisconnected + switchDelay <= System.currentTimeMillis()) {
					if (activeEss != null) {
						connect(activeEss);
					} else {
						state = State.DISCONNECTING;
					}
				}
			}
		}
		break;
		case DISCONNECTED: {
			Ess mostLoad = getLargestSoc();
			// only connect if soc is larger than minSoc + 5 or Ess is On-Grid
			if (mostLoad != null) {
				if (mostLoad.soc.value() > mostLoad.minSoc.value() + 5) {
					// connect(mostLoad);
					activeEss = mostLoad;
					activeEss.start();
					activeEss.setActiveSupplybus(this);
					lastTimeDisconnected = System.currentTimeMillis();
					state = State.CONNECTING;
				}
			} else {
				// all ess empty check if On-Grid
				List<Ess> onGridEss = getOnGridEss();
				if (onGridEss.size() > 0) {
					// connect(mostLoad);
					activeEss = onGridEss.get(0);
					activeEss.start();
					activeEss.setActiveSupplybus(this);
					lastTimeDisconnected = System.currentTimeMillis();
					state = State.CONNECTING;
				} else {
					logError("no ess to connect");
				}
			}
			if (supplybusOnIndication != null) {
				try {
					supplybusOnIndication.pushWrite(0L);
				} catch (WriteChannelException e) {
					logError("can't set supplybusOnIndication: " + e.getMessage());
				}
			}
		}
		break;
		case DISCONNECTING: {
			// if not disconnected send disconnect command again
			if (isDisconnected()) {
				state = State.DISCONNECTED;
			} else {
				// disconnect all loads before disconnection
				try {
					if (disconnectLoads()) {
						disconnect();
						try {
							Ess active = getActiveEss();
							if (active != null && !active.equals(primaryEss)) {
								active.standby();
							}
						} catch (SupplyBusException e) {
							logError("get Active Ess failed: " + e.getMessage());
						}
						if (supplybusOnIndication != null) {
							try {
								supplybusOnIndication.pushWrite(0L);
							} catch (WriteChannelException e) {
								logError("can't set supplybusOnIndication: " + e.getMessage());
							}
						}
					}
				} catch (WriteChannelException e) {
					logWarn("Can't stop load: " + e.getMessage());
				}
			}
		}
		break;
		case UNKNOWN: {
			try {
				activeEss = getActiveEss();
				if (activeEss != null && activeEss.getActiveSupplybus() == null) {
					state = State.CONNECTED;
					activeEss.setActiveSupplybus(this);
					activeEss.start();
				} else {
					state = State.DISCONNECTING;
				}
			} catch (SupplyBusException e) {
				disconnect();
			}
		}
		break;
		default:
			break;

		}
		primaryEss.start();
	}

	public Ess getActiveEss() throws SupplyBusException, InvalidValueException {
		List<Ess> activeEss = new ArrayList<>();
		for (Entry<Ess, WriteChannel<Boolean>> entry : switchEssMapping.entrySet()) {
			if (entry.getValue().value()) {
				activeEss.add(entry.getKey());
			}
		}
		if (activeEss.size() > 1) {
			throw new SupplyBusException("there are more than one ess connected to the supply bus.", activeEss, this);
		} else if (activeEss.size() == 0) {
			return null;
		} else {
			return activeEss.get(0);
		}
	}

	public void disconnect() {
		for (Entry<Ess, WriteChannel<Boolean>> entry : switchEssMapping.entrySet()) {
			try {
				entry.getValue().pushWrite(false);
			} catch (WriteChannelException e) {
				logError("Failed to write disconnect command to digital output " + entry.getValue().address() + ": "
						+ e.getMessage());
			}
		}
		if (activeEss != null) {
			activeEss.setActiveSupplybus(null);
		}
		activeEss = null;
	}

	public boolean isConnected() {
		if (activeEss != null) {
			WriteChannel<Boolean> sOn = switchEssMapping.get(activeEss);
			if (sOn.valueOptional().isPresent() && sOn.valueOptional().get()) {
				return true;
			}
		}
		return false;
	}

	public boolean isDisconnected() throws InvalidValueException {
		try {
			if (getActiveEss() == null) {
				return true;
			}
		} catch (SupplyBusException e) {
			return false;
		}
		return false;
	}

	public void connect(Ess ess) throws InvalidValueException {
		try {
			if (getActiveEss() == null && ess != null) {
				activeEss = ess;
				activeEss.setActiveSupplybus(this);
				WriteChannel<Boolean> sOn = switchEssMapping.get(ess);
				try {
					sOn.pushWrite(true);
				} catch (WriteChannelException e) {
					logError("Failed to write connect command to digital output " + sOn.address() + ": "
							+ e.getMessage());
				}
			}
		} catch (SupplyBusException e) {
			logError("can't connect ess: " + e.getMessage());
		}
	}

	public Ess getLargestSoc() {
		List<Ess> esss = new ArrayList<>(switchEssMapping.keySet());
		// remove empty ess and fault ess
		for (Iterator<Ess> iter = esss.iterator(); iter.hasNext();) {
			Ess ess = iter.next();
			try {
				if (ess.soc.value() < ess.minSoc.value()
						|| ess.systemState.labelOptional().equals(Optional.of(EssNature.FAULT))
						|| ess.getActiveSupplybus() != null) {
					iter.remove();
				}
			} catch (InvalidValueException e) {
				iter.remove();
			}
		}
		// if (esss.size() > 1) {
		// esss.remove(primaryEss);
		// }
		Ess largestSoc = null;
		for (Ess ess : esss) {
			try {
				if (largestSoc == null || largestSoc.useableSoc() < ess.useableSoc()) {
					largestSoc = ess;
				}
			} catch (InvalidValueException e) {
				logError("failed to read soc of " + ess.id() + ": " + e.getMessage());
			}
		}
		return largestSoc;
	}

	private List<Ess> getOnGridEss() {
		List<Ess> esss = new ArrayList<>(switchEssMapping.keySet());
		for (Iterator<Ess> iter = esss.iterator(); iter.hasNext();) {
			Ess ess = iter.next();
			if (ess.gridMode.labelOptional().equals(Optional.of(EssNature.OFF_GRID))
					|| ess.systemState.labelOptional().equals(Optional.of(EssNature.FAULT))
					|| ess.getActiveSupplybus() != null) {
				logInfo("Ess [" + ess.id() + "] is unusable. GridMode ["
						+ ess.gridMode.labelOptional().orElse("UNDEFINED") + "] SystemState ["
						+ ess.systemState.labelOptional().orElse("UNDEFINED") + "] ActiveSupplybus ["
						+ ess.getActiveSupplybus() + "]");
				iter.remove();
			}
		}
		if (esss.size() == 0) {
			logError("No OnGrid Ess!");
		}
		return esss;
	}

	private boolean disconnectLoads() throws InvalidValueException, WriteChannelException {
		if (timeLoadSwitched + switchDelay <= System.currentTimeMillis()) {
			if (loadIndex < loads.size()) {
				WriteChannel<Long> load = loads.get(loadIndex);
				if (load.value() == 0L) {
					loadIndex++;
				} else {
					loadState[loadIndex] = false;
					timeLoadSwitched = System.currentTimeMillis();
				}
				return false;
			} else {
				loadIndex = 0;
				for (WriteChannel<Long> load : loads) {
					if (load.value() != 0L) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	private boolean connectLoads() throws WriteChannelException, InvalidValueException {
		if (timeLoadSwitched + switchDelay <= System.currentTimeMillis()) {
			if (loadIndex < loads.size()) {
				if (loadState[loadIndex] != null && loadState[loadIndex] == true) {
					loadIndex++;
				} else {
					loadState[loadIndex] = true;
					timeLoadSwitched = System.currentTimeMillis();
				}
				return false;
			} else {
				loadIndex = 0;
				for (Boolean load : loadState) {
					if (!load) {
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	private void logInfo(String message) {
		this.log.info(this.name + ": " + message);
	}

	private void logWarn(String message) {
		this.log.warn(this.name + ": " + message);
	}

	private void logError(String message) {
		this.log.error(this.name + ": " + message);
	}

}
