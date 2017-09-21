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
package io.openems.impl.device.simulator;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.simulator.SimulatorDevice;

@ThingInfo(title = "Simulator")
public class Simulator extends SimulatorDevice {

	/*
	 * Constructors
	 */
	public Simulator(Bridge parent) throws OpenemsException {
		super(parent);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "symmetric Ess", description = "Sets the symmetric Ess nature.", type = SimulatorSymmetricEss.class, isOptional = true)
	public final ConfigChannel<SimulatorSymmetricEss> symmetricEss = new ConfigChannel<>("symmetricEss", this);
	@ChannelInfo(title = "asymmetric Ess", description = "Sets the asymmetric Ess nature.", type = SimulatorAsymmetricEss.class, isOptional = true)
	public final ConfigChannel<SimulatorAsymmetricEss> asymmetricEss = new ConfigChannel<>("asymmetricEss", this);

	@ChannelInfo(title = "Charger", description = "Sets the Charger nature.", type = SimulatorCharger.class, isOptional = true)
	public final ConfigChannel<SimulatorCharger> charger = new ConfigChannel<>("charger", this);

	@ChannelInfo(title = "Grid-Meter", description = "Sets the grid meter nature.", type = SimulatorGridMeter.class, isOptional = true)
	public final ConfigChannel<SimulatorGridMeter> gridMeter = new ConfigChannel<>("gridMeter", this);

	@ChannelInfo(title = "Production-Meter", description = "Sets the production meter nature.", type = SimulatorProductionMeter.class, isOptional = true)
	public final ConfigChannel<SimulatorProductionMeter> productionMeter = new ConfigChannel<>("productionMeter", this);

	@ChannelInfo(title = "Sps", description = "Sets the Riedmann sps nature.", type = SimulatorRiedmannNature.class, isOptional = true)
	public final ConfigChannel<SimulatorRiedmannNature> sps = new ConfigChannel<>("sps", this);

	@ChannelInfo(title = "Output", description = "Sets the output nature.", type = SimulatorOutput.class, isOptional = true)
	public final ConfigChannel<SimulatorOutput> output = new ConfigChannel<>("output", this);

	/*
	 * Methods
	 */
	@Override
	protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (symmetricEss.valueOptional().isPresent()) {
			natures.add(symmetricEss.valueOptional().get());
		}
		if (asymmetricEss.valueOptional().isPresent()) {
			natures.add(asymmetricEss.valueOptional().get());
		}
		if (gridMeter.valueOptional().isPresent()) {
			natures.add(gridMeter.valueOptional().get());
		}
		if (productionMeter.valueOptional().isPresent()) {
			natures.add(productionMeter.valueOptional().get());
		}
		if (charger.valueOptional().isPresent()) {
			natures.add(charger.valueOptional().get());
		}
		if (sps.valueOptional().isPresent()) {
			natures.add(sps.valueOptional().get());
		}
		if (output.valueOptional().isPresent()) {
			natures.add(output.valueOptional().get());
		}
		return natures;
	}

}
