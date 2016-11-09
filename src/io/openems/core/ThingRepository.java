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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Table;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.persistence.Persistence;
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

	private ThingRepository() {}

	private final BiMap<String, Thing> thingIds = HashBiMap.create();
	private HashMultimap<Class<? extends Thing>, Thing> thingClasses = HashMultimap.create();
	private Set<Bridge> bridges = new HashSet<>();
	private Set<Persistence> persistences = new HashSet<>();
	private final Table<Thing, String, Channel> thingChannels = HashBasedTable.create();
	private HashMultimap<Thing, ConfigChannel<?>> thingConfigChannels = HashMultimap.create();
	private HashMultimap<Thing, WriteChannel<?>> thingWriteChannels = HashMultimap.create();

	/**
	 * Add a Thing to the Repository and cache its Channels and other information for later usage.
	 *
	 * @param thing
	 */
	public synchronized void addThing(Thing thing) {
		if (thingIds.containsValue(thing)) {
			// Thing was already added
			return;
		}
		// Add to thingIds
		thingIds.forcePut(thing.id(), thing);

		// Add to thingClasses
		thingClasses.put(thing.getClass(), thing);

		// Add to bridges
		if (thing instanceof Bridge) {
			bridges.add((Bridge) thing);
		}

		// Add to persistences
		if (thing instanceof Persistence) {
			persistences.add((Persistence) thing);
		}

		// Add Channels to thingChannels, thingConfigChannels and thingWriteChannels
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
				if (channel == null) {
					log.error(
							"Channel is returning null! Thing [" + thing.id() + "], Member [" + member.getName() + "]");
					continue;
				}
				// Add Channel to thingChannels
				thingChannels.put(thing, channel.id(), channel);

				// Add Channel to configChannels
				if (channel instanceof ConfigChannel) {
					thingConfigChannels.put(thing, (ConfigChannel<?>) channel);
				}

				// Add Channel to writeChannels
				if (channel instanceof WriteChannel) {
					thingWriteChannels.put(thing, (WriteChannel<?>) channel);
				}

				// Register Databus as listener
				if (channel instanceof ReadChannel) {
					Databus databus = Databus.getInstance();
					((ReadChannel<?>) channel).listener(databus);
				}

			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				log.warn("Unable to add Channel. Member [" + member.getName() + "]", e);
			}
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
	 * Returns all Channels for this Thing.
	 *
	 * @param thing
	 * @return
	 */
	public synchronized Collection<Channel> getChannels(Thing thing) {
		return Collections.unmodifiableCollection(thingChannels.row(thing).values());
	}

	/**
	 * Returns all Config-Channels for this Thing.
	 *
	 * @param thing
	 * @return
	 */
	public synchronized Set<ConfigChannel<?>> getConfigChannels(Thing thing) {
		return Collections.unmodifiableSet(thingConfigChannels.get(thing));
	}

	/**
	 * Returns all Write-Channels for this Thing.
	 *
	 * @param thing
	 * @return
	 */
	public synchronized Set<WriteChannel<?>> getWriteChannels(Thing thing) {
		return Collections.unmodifiableSet(thingWriteChannels.get(thing));
	}

	/**
	 * Returns all Write-Channels.
	 *
	 * @param thing
	 * @return
	 */
	public synchronized Collection<WriteChannel<?>> getWriteChannels() {
		return Collections.unmodifiableCollection(thingWriteChannels.values());
	}

	/**
	 * Returns all Persistence-Workers.
	 *
	 * @param thing
	 * @return
	 */
	public synchronized Set<Persistence> getPersistences() {
		return Collections.unmodifiableSet(persistences);
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

	public Optional<Channel> getChannel(String thingId, String channelId) {
		Thing thing = thingIds.get(thingId);
		if (thing == null) {
			return Optional.empty();
		}
		Channel channel = thingChannels.row(thing).get(channelId);
		return Optional.ofNullable(channel);
	}
}
