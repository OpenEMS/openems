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
package io.openems.impl.protocol.studer.internal.object;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.api.thing.Thing;
import io.openems.impl.protocol.studer.internal.property.StuderProperty;

public abstract class StuderObject<T> {

	protected final Logger log;

	protected final int objectId;
	protected final ObjectType objectType;
	protected final String name;
	protected final String unit;
	protected final Thing parent;

	public StuderObject(int objectId, String name, String unit, Thing parent, ObjectType objectType) {
		log = LoggerFactory.getLogger(this.getClass());
		this.objectId = objectId;
		this.objectType = objectType;
		this.name = name;
		this.unit = unit;
		this.parent = parent;
	}

	public int getObjectId() {
		return objectId;
	}

	public ObjectType getObjectType() {
		return objectType;
	}

	public abstract StuderProperty<T>[] getProperties();
}
