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
package io.openems.impl.device.system;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.DebugChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.OpenemsException;
import io.openems.impl.protocol.system.SystemDevice;

@ThingInfo(title = "Operating system")
public class System extends SystemDevice {

	/*
	 * Constructors
	 */
	public System(Bridge parent) throws OpenemsException {
		super(parent);
	}

	/*
	 * Config
	 */
	@ChannelInfo(title = "System", description = "Sets the system nature.", type = SystemNature.class)
	public final ConfigChannel<SystemNature> system = new ConfigChannel<>("system", this);
	@ChannelInfo(title = "Debug", description = "Enables DebugChannels to write into database", type = Boolean.class, isOptional = true, defaultValue = "false")
	public final ConfigChannel<Boolean> debug = new ConfigChannel<Boolean>("debug", this)
			.addChangeListener(new ChannelChangeListener() {

				@Override
				public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
					if (newValue.isPresent() && (boolean) newValue.get()) {
						DebugChannel.enableDebug();
					} else {
						DebugChannel.disableDebug();
					}
				}
			});

	/*
	 * Methods
	 */
	@Override
	protected Set<DeviceNature> getDeviceNatures() {
		Set<DeviceNature> natures = new HashSet<>();
		if (system.valueOptional().isPresent()) {
			natures.add(system.valueOptional().get());
		}
		return natures;
	}

}
