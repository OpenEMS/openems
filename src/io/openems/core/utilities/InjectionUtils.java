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
package io.openems.core.utilities;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Optional;

import io.openems.api.channel.IsChannel;
import io.openems.api.exception.ConfigException;
import io.openems.api.thing.Thing;

public class InjectionUtils {

	/**
	 * Creates an instance of the given {@link Class}. {@link Object} arguments are optional.
	 *
	 * Restriction: this implementation tries only the first constructor of the Class.
	 *
	 * @param clazz
	 * @param args
	 * @return
	 * @throws ConfigException
	 */
	public static Object getInstance(Class<?> clazz, Object... args) throws ConfigException {
		try {
			if (args.length == 0) {
				return clazz.newInstance();
			} else {
				Constructor<?> constructor = clazz.getConstructors()[0];
				return constructor.newInstance(args);
			}
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			throw new ConfigException("Unable to instantiate class [" + clazz.getName() + "]: " + e.getMessage());
		}
	}

	/**
	 * Searches the class tree for a method with the name methodName, that is annotated with IsChannel.
	 *
	 * @param clazz
	 * @param methodName
	 * @return IsChannel annotation or null if no match was found
	 */
	public static Optional<IsChannel> getIsChannelMethods(Class<?> clazz, String methodName) {
		// clazz must be a Thing
		if (!Thing.class.isInterface() && !Thing.class.isAssignableFrom(clazz)) {
			return Optional.empty();
		}
		// check if method can be found
		Method method;
		try {
			method = clazz.getMethod(methodName);
		} catch (NoSuchMethodException | SecurityException e) {
			return Optional.empty();
		}
		// return the annotation if found
		if (method.isAnnotationPresent(IsChannel.class)) {
			return Optional.of(method.getAnnotation(IsChannel.class));
		}
		// start recursive search if not found
		for (Class<?> implementedInterface : clazz.getInterfaces()) {
			// search all implemented interfaces
			Optional<IsChannel> ret = getIsChannelMethods(implementedInterface, methodName);
			if (ret.isPresent()) {
				return ret;
			}
		}
		if (clazz.getSuperclass() == null) {
			// reached the top end... no superclass found
			return Optional.empty();
		}
		return getIsChannelMethods(clazz.getSuperclass(), methodName);
	}
}
