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
package io.openems.core;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.thing.Thing;

public class ThingRepository {
	private final static Logger log = LoggerFactory.getLogger(ThingRepository.class);

	private static ThingRepository instance;

	public static synchronized ThingRepository getInstance() {
		if (ThingRepository.instance == null) {
			ThingRepository.instance = new ThingRepository();
		}
		return ThingRepository.instance;
	}

	private ThingRepository() {
		this.databus = Databus.getInstance();
	}

	private final Databus databus;
	private BiMap<String, Thing> thingIds = HashBiMap.create();
	private HashMultimap<Thing, Channel> thingChannels = HashMultimap.create();
	private HashMultimap<Thing, ConfigChannel<?>> thingConfigChannels = HashMultimap.create();
	private HashMultimap<Class<? extends Thing>, Thing> thingClasses = HashMultimap.create();
	private Set<Bridge> bridges = new HashSet<>();

	public synchronized void addThing(Thing thing) {
		thingIds.forcePut(thing.id(), thing);
		thingClasses.put(thing.getClass(), thing);
		if (thing instanceof Bridge) {
			bridges.add((Bridge) thing);
		}
	}

	/**
	 * Get all declared members of thing class.
	 *
	 * @param clazz
	 * @return
	 */
	private static List<Member> getMembers(Class<? extends Thing> clazz) {
		List<Member> members = new LinkedList<>();
		for (Method method : clazz.getMethods()) {
			members.add(method);
		}
		for (Field field : clazz.getFields()) {
			members.add(field);
		}
		return Collections.unmodifiableList(members);
	}

	/**
	 * Returns all Channels for this Thing. Result is cached for later usage.
	 *
	 * @param thing
	 * @return
	 */
	public synchronized Set<Channel> getChannels(Thing thing) {
		addThing(thing);
		if (!thingChannels.containsKey(thing)) {
			// Channels for this Thing were not yet parsed.
			List<Member> members = getMembers(thing.getClass());
			for (Member member : members) {
				try {
					Channel channel;
					if (member instanceof Method && Channel.class.isAssignableFrom(((Method) member).getReturnType())) {
						// It's a Method with ReturnType Channel
						channel = (Channel) ((Method) member).invoke(thing);

					} else if (member instanceof Field && Channel.class.isAssignableFrom(((Field) member).getType())) {
						// It's a Field with Type Channel
						channel = (Channel) ((Field) member).get(thing);
					} else {
						continue;
					}
					// Store Channel in cache
					thingChannels.put(thing, channel);

					// Register Databus as listener
					if (channel instanceof ReadChannel) {
						((ReadChannel<?>) channel).listener(databus);
					}

				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					log.warn("Unable to add Channel. Member [" + member.getName() + "]", e);
				}
			}

		}
		return Collections.unmodifiableSet(thingChannels.get(thing));

	}

	/**
	 * Returns all Config-Channels for this Thing. Result is cached for later usage.
	 *
	 * @param thing
	 * @return
	 */
	public synchronized Set<ConfigChannel<?>> getConfigChannels(Thing thing) {
		addThing(thing);
		if (!thingConfigChannels.containsKey(thing)) {
			// Config-Channels for this Thing were not yet received. Filter from all Channels.
			Set<Channel> channels = getChannels(thing);
			for (Channel channel : channels) {
				if (channel instanceof ConfigChannel) {
					thingConfigChannels.put(thing, (ConfigChannel<?>) channel);
				}
			}
		}
		return Collections.unmodifiableSet(thingConfigChannels.get(thing));
	}

	public synchronized Set<Thing> getThingsByClass(Class<? extends Thing> clazz) {
		return Collections.unmodifiableSet(thingClasses.get(clazz));
	}

	public synchronized Set<Thing> getThingsAssignableByClass(Class<? extends Thing> clazz) {
		Set<Thing> things = new HashSet<>();
		for (Class<? extends Thing> subclazz : thingClasses.keySet()) {
			if (clazz.isAssignableFrom(subclazz)) {
				things.addAll(thingClasses.get(subclazz));
			}

		}
		return Collections.unmodifiableSet(things);
	}

	public synchronized Optional<Thing> getThingById(String id) {
		return Optional.ofNullable(thingIds.get(id));
	}

	public synchronized Set<Bridge> getBridges() {
		return Collections.unmodifiableSet(bridges);
	}
}
