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

import io.openems.api.exception.ConfigException;
import io.openems.api.exception.ReflectionException;
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
	public static Object getInstance(Class<?> clazz, Object... args) throws ReflectionException {
		try {
			if (args.length == 0) {
				return clazz.newInstance();
			} else {
				Constructor<?> constructor = clazz.getConstructors()[0];
				return constructor.newInstance(args);
			}
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			throw new ReflectionException("Unable to instantiate class [" + clazz.getName() + "]: " + e.getMessage());
		}
	}

	/**
	 * Creates a Thing instance of the given {@link Class}. {@link Object} arguments are optional.
	 *
	 * @param clazz
	 * @param args
	 * @return
	 * @throws CastException
	 * @throws ConfigException
	 * @throws ReflectionException
	 */
	public static Thing getThingInstance(Class<?> clazz, Object... args) throws ReflectionException {
		try {
			return (Thing) InjectionUtils.getInstance(clazz, args);
		} catch (ClassCastException e) {
			e.printStackTrace();
			throw new ReflectionException("Class [" + clazz.getName() + "] is not a Thing");
		}
	}

	/**
	 * Creates an instance of the given {@link Class}name. Uses {@link getThingInstance()} internally. {@link Object}
	 * arguments are optional.
	 *
	 * @param className
	 * @return
	 * @throws CastException
	 * @throws ConfigException
	 */
	@SuppressWarnings("unchecked") public static Thing getThingInstance(String className, Object... args)
			throws ReflectionException {
		Class<? extends Thing> clazz;
		try {
			clazz = (Class<? extends Thing>) Class.forName(className);
		} catch (ClassNotFoundException e) {
			throw new ReflectionException("Class not found: [" + className + "]");
		}
		return getThingInstance(clazz, args);
	}
}
