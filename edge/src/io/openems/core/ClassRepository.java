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
package io.openems.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Iterables;

import io.openems.api.bridge.Bridge;
import io.openems.api.channel.Channel;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.Device;
import io.openems.api.doc.ChannelDoc;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingDoc;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.ReflectionException;
import io.openems.api.scheduler.Scheduler;
import io.openems.api.thing.Thing;
import io.openems.core.utilities.ConfigUtils;
import io.openems.core.utilities.StringUtils;

/**
 * Retreives and caches information about classes via reflection
 *
 * @author stefan.feilmeier
 */
public class ClassRepository {
	private final static Logger log = LoggerFactory.getLogger(ClassRepository.class);
	private static ClassRepository instance;

	public static synchronized ClassRepository getInstance() {
		if (ClassRepository.instance == null) {
			ClassRepository.instance = new ClassRepository();
		}
		return ClassRepository.instance;
	}

	private Set<Class<? extends Bridge>> bridges = new HashSet<>();
	private Set<Class<? extends Scheduler>> schedulers = new HashSet<>();
	private Set<Class<? extends Device>> devices = new HashSet<>();
	private Set<Class<? extends Controller>> controllers = new HashSet<>();
	private HashMap<Class<? extends Thing>, ThingDoc> thingDocs = new HashMap<>();

	public ClassRepository() {}

	public Iterable<ThingDoc> getAvailableThings() throws ReflectionException {
		return Iterables.concat( //
				getAvailableBridges(), //
				getAvailableControllers(), //
				getAvailableDevices(), //
				getAvailableSchedulers());
	}

	@SuppressWarnings("unchecked")
	public Collection<ThingDoc> getAvailableControllers() throws ReflectionException {
		// update cache of available controllers
		if (this.controllers.isEmpty()) {
			for (Class<? extends Thing> clazz : ConfigUtils.getAvailableClasses("io.openems.impl.controller",
					Controller.class, "Controller")) {
				this.controllers.add((Class<? extends Controller>) clazz);
			}
		}
		// create result
		Collection<ThingDoc> controllerDocs = new ArrayList<>();
		for (Class<? extends Controller> clazz : this.controllers) {
			controllerDocs.add(this.getThingDoc(clazz));
		}
		return Collections.unmodifiableCollection(controllerDocs);
	}

	@SuppressWarnings("unchecked")
	public Collection<ThingDoc> getAvailableBridges() throws ReflectionException {
		// update cache of available bridges
		if (this.bridges.isEmpty()) {
			for (Class<? extends Thing> clazz : ConfigUtils.getAvailableClasses("io.openems.impl.protocol",
					Bridge.class, "")) {
				this.bridges.add((Class<? extends Bridge>) clazz);
			}
		}
		// create result
		Collection<ThingDoc> bridgeDocs = new ArrayList<>();
		for (Class<? extends Bridge> clazz : this.bridges) {
			bridgeDocs.add(this.getThingDoc(clazz));
		}
		return Collections.unmodifiableCollection(bridgeDocs);
	}

	@SuppressWarnings("unchecked")
	public Collection<ThingDoc> getAvailableDevices() throws ReflectionException {
		// update cache of available bridges
		if (devices.isEmpty()) {
			for (Class<? extends Thing> clazz : ConfigUtils.getAvailableClasses("io.openems.impl.device", Device.class,
					"")) {
				this.devices.add((Class<? extends Device>) clazz);
			}
		}
		// create result
		Collection<ThingDoc> deviceDocs = new ArrayList<>();
		for (Class<? extends Device> clazz : this.devices) {
			deviceDocs.add(this.getThingDoc(clazz));
		}
		return Collections.unmodifiableCollection(deviceDocs);
	}

	@SuppressWarnings("unchecked")
	public Collection<ThingDoc> getAvailableSchedulers() throws ReflectionException {
		// update cache of available bridges
		if (this.schedulers.isEmpty()) {
			for (Class<? extends Thing> clazz : ConfigUtils.getAvailableClasses("io.openems.impl.scheduler",
					Scheduler.class, "Scheduler")) {
				this.schedulers.add((Class<? extends Scheduler>) clazz);
			}
		}
		// create result
		Collection<ThingDoc> schedulerDocs = new ArrayList<>();
		for (Class<? extends Device> clazz : this.devices) {
			schedulerDocs.add(this.getThingDoc(clazz));
		}
		return Collections.unmodifiableCollection(schedulerDocs);
	}

	/**
	 * Returns the cached ThingDoc or parses the class and adds it to the cache.
	 * Field annotations have higher priority than method annotations!
	 *
	 * @param clazz
	 */
	public ThingDoc getThingDoc(Class<? extends Thing> clazz) {
		if (this.thingDocs.containsKey(clazz)) {
			// return from cache
			return this.thingDocs.get(clazz);
		}
		ThingDoc thingDoc = new ThingDoc(clazz);

		// get info about thing
		ThingInfo thing = clazz.getAnnotation(ThingInfo.class);
		if (thing == null) {
			log.warn("Thing [" + clazz.getName() + "] has no @ThingInfo annotation");
		} else {
			thingDoc.setThingDescription(thing);
		}

		// parse all methods
		for (Method method : clazz.getMethods()) {
			Class<?> type = null;
			if (method.getReturnType().isArray()) {
				Class<?> rtype = method.getReturnType();
				type = rtype.getComponentType();
			} else {
				type = method.getReturnType();
			}
			if (Channel.class.isAssignableFrom(type)) {
				Optional<ChannelInfo> channelInfoOpt = getAnnotationForMethod(clazz, method.getName());
				String channelId = StringUtils.capitalizeFirstLetter(method.getName());
				ChannelDoc channelDoc = new ChannelDoc(method, channelId, channelInfoOpt);
				thingDoc.addChannelDoc(channelDoc);
				if (ConfigChannel.class.isAssignableFrom(type)) {
					thingDoc.addConfigChannelDoc(channelDoc);
				}
			}
		}
		// parse all fields
		for (Field field : clazz.getFields()) {
			Class<?> type = field.getType();
			if (Channel.class.isAssignableFrom(type)) {
				ChannelDoc channelDoc = new ChannelDoc(field, field.getName(),
						Optional.ofNullable(field.getAnnotation(ChannelInfo.class)));
				thingDoc.addChannelDoc(channelDoc);
				if (ConfigChannel.class.isAssignableFrom(type)) {
					thingDoc.addConfigChannelDoc(channelDoc);
				}
			}
		}
		return thingDoc;
	}

	/**
	 * Tries to find the annotation of the method in the class hierarchy
	 *
	 * @param clazz
	 * @return
	 */
	private Optional<ChannelInfo> getAnnotationForMethod(Class<?> clazz, String methodName) {
		Method method;
		try {
			method = clazz.getMethod(methodName);
		} catch (NoSuchMethodException | SecurityException e) {
			return Optional.empty();
		}
		if (method.isAnnotationPresent(ChannelInfo.class)) {
			// found annotation
			return Optional.of(method.getAnnotation(ChannelInfo.class));
		} else {
			Class<?> superclazz = clazz.getSuperclass();
			if (superclazz != null) {
				Optional<ChannelInfo> channelInfo = getAnnotationForMethod(superclazz, methodName);
				if (channelInfo.isPresent()) {
					return channelInfo;
				}
			}
			for (Class<?> iface : clazz.getInterfaces()) {
				Optional<ChannelInfo> channelInfo = getAnnotationForMethod(iface, methodName);
				if (channelInfo.isPresent()) {
					return channelInfo;
				}
			}
		}
		return Optional.empty();
	}
}
