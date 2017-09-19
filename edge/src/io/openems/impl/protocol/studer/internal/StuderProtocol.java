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
package io.openems.impl.protocol.studer.internal;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.openems.api.channel.Channel;
import io.openems.impl.protocol.studer.internal.object.StuderObject;
import io.openems.impl.protocol.studer.internal.property.ReadProperty;
import io.openems.impl.protocol.studer.internal.property.StuderProperty;
import io.openems.impl.protocol.studer.internal.property.WriteProperty;

/**
 * Holds the protocol definition of a Studer device
 *
 * @author stefan.feilmeier
 */
public class StuderProtocol {

	// private static Logger log = LoggerFactory.getLogger(StuderProtocol.class);

	private final Map<Channel, StuderProperty<?>> channelPropertyMap = new ConcurrentHashMap<>();
	private final Set<ReadProperty<?>> readProperties = ConcurrentHashMap.newKeySet();
	// requiredProperties stays empty till someone calls "setAsRequired()"
	private final Set<WriteProperty<?>> writableProperties = ConcurrentHashMap.newKeySet();
	// private final Set<ReadProperty<?>> otherProperties = ConcurrentHashMap.newKeySet();
	// private final LinkedList<ReadProperty<?>> otherPropertiesQueue = new LinkedList<>();

	public StuderProtocol(StuderObject<?>... objects) {
		for (StuderObject<?> object : objects) {
			addObject(object);
		}
	}

	public void addObject(StuderObject<?> object) {
		for (StuderProperty<?> property : object.getProperties()) {
			// fillchannelPropertyMap
			Channel channel = property.channel();
			if (channel != null) {
				channelPropertyMap.put(channel, property);
			}
			if (property instanceof WriteProperty) {
				// fill writableProperties
				writableProperties.add((WriteProperty<?>) property);
			}
			if (property instanceof ReadProperty) {
				// fill otherProperties
				readProperties.add((ReadProperty<?>) property);
			}
		}
	}

	// public Optional<ReadProperty<?>> getNextOtherProperty() {
	// if (otherPropertiesQueue.isEmpty()) {
	// otherPropertiesQueue.addAll(otherProperties);
	// }
	// ReadProperty<?> property = otherPropertiesQueue.poll();
	// return Optional.ofNullable(property);
	// }

	public Collection<ReadProperty<?>> getReadProperties() {
		return Collections.unmodifiableSet(readProperties);
	}

	public Collection<WriteProperty<?>> getWritableProperties() {
		return Collections.unmodifiableSet(writableProperties);
	}

	public StuderProperty<?> getPropertyByChannel(Channel channel) {
		return channelPropertyMap.get(channel);
	}

	// public void setAsRequired(Channel channel) {
	// StuderProperty<?> property = channelPropertyMap.get(channel);
	// if (property != null && property instanceof ReadProperty) {
	// otherProperties.remove(property);
	// readProperties.add((ReadProperty<?>) property);
	// }
	// }
}
