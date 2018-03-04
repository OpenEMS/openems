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
package io.openems.api.doc;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Set;

import com.google.common.collect.Sets;

import io.openems.common.session.Role;

@Retention(RUNTIME)
@Target({ FIELD, METHOD })
public @interface ChannelInfo {

	public static final String DEFAULT_DESCRIPTION = "";
	public static final String DEFAULT_TITLE = "";
	public static final boolean DEFAULT_IS_OPTIONAL = false;
	public static final boolean DEFAULT_IS_ARRAY = false;
	public static final Set<Role> DEFAULT_READ_ROLES = Sets.newHashSet(Role.GUEST, Role.OWNER, Role.INSTALLER,
			Role.ADMIN);
	public static final Set<Role> DEFAULT_WRITE_ROLES = Sets.newHashSet(Role.ADMIN);
	public static final String DEFAULT_VALUE = "";

	String title() default DEFAULT_TITLE;

	String description() default DEFAULT_DESCRIPTION;

	Class<?> type();

	boolean isOptional() default DEFAULT_IS_OPTIONAL;

	boolean isArray() default DEFAULT_IS_ARRAY;

	/**
	 * By default all roles are allowed to read. ADMIN is added automatically to this list.
	 *
	 * @return
	 */
	Role[] readRoles() default { Role.GUEST, Role.OWNER, Role.INSTALLER, Role.ADMIN };
	/**
	 * By default only the "ADMIN" role is allowed to write. ADMIN is added automatically to this list.
	 *
	 * @return
	 */
	Role[] writeRoles() default { Role.ADMIN };

	/**
	 * String is interpreted as a JsonElement
	 *
	 * @return
	 */
	String defaultValue() default DEFAULT_VALUE;
}
