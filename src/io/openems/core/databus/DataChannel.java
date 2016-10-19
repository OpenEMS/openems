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
package io.openems.core.databus;

import io.openems.api.channel.Channel;
import io.openems.api.thing.Thing;

public class DataChannel {
	public final Channel channel;
	public final String channelId;
	public final Thing thing;
	public final String thingId;

	public DataChannel(Thing thing, String thingId, Channel channel, String channelId) {
		this.thing = thing;
		this.thingId = thingId;
		this.channel = channel;
		this.channelId = channelId;
	}

	@Override
	public String toString() {
		return "DataChannelMapping [channel=" + channel + ", channelId=" + channelId + "]";
	}
}
