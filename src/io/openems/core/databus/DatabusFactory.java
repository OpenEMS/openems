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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.channel.Channel;
import io.openems.api.channel.IsChannel;
import io.openems.api.thing.Thing;
import io.openems.core.utilities.InjectionUtils;

public class DatabusFactory {
	private static Logger log = LoggerFactory.getLogger(DatabusFactory.class);

	public static Map<String, DataChannel> getDataChannels(Thing thing, Databus databus) {
		HashMap<String, DataChannel> dataChannels = new HashMap<>();

		// Get all declared members of thing class
		List<Member> members = new LinkedList<>();
		for (Method method : thing.getClass().getDeclaredMethods()) {
			members.add(method);
		}
		for (Field field : thing.getClass().getDeclaredFields()) {
			members.add(field);
		}

		// fill channels for this thing
		for (Member member : members) {

			// is this member a Channel type?
			boolean isChannelType = false;
			if (member instanceof Method) {
				// is Method ReturnType a Channel?
				isChannelType = Channel.class.isAssignableFrom(((Method) member).getReturnType());
			} else if (member instanceof Field) {
				// is Field a Channel?
				isChannelType = Channel.class.isAssignableFrom(((Field) member).getType());
			}

			if (isChannelType) {
				// Check if Member has a IsChannel annotation
				Optional<IsChannel> annotation = InjectionUtils.getIsChannelMembers(thing.getClass(), member.getName());
				if (annotation.isPresent()) {
					try {
						// Ok. Now receive the Channel
						Channel<?> channel = null;
						if (member instanceof Method) {
							channel = (Channel<?>) ((Method) member).invoke(thing);
						} else if (member instanceof Field) {
							channel = (Channel<?>) ((Field) member).get(thing);
						}

						if (channel != null) {
							// Connect the channel to databus
							channel.setDatabus(databus);
							DataChannel dataChannel = new DataChannel(thing, thing.getThingId(), channel,
									annotation.get().id());
							dataChannels.put(annotation.get().id(), dataChannel);
						}
					} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
						log.warn("Unable to add Channel to Databus. Member [" + member.getName() + "], ChannelId ["
								+ annotation.get().id() + "]: " + e.getMessage());
					}
				}
			}
		}
		return dataChannels;
	}

}
