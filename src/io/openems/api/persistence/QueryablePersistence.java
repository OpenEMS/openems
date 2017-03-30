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
package io.openems.api.persistence;

import java.time.ZonedDateTime;

import com.google.gson.JsonObject;

import io.openems.api.exception.OpenemsException;

public abstract class QueryablePersistence extends Persistence {

	/**
	 *
	 *
	 * @param fromDate
	 * @param toDate
	 * @param channels
	 * @param resolution
	 *            in seconds
	 * @return
	 * @throws OpenemsException
	 *
	 *             <pre>
	 * Returns:
	 * [{
	 *   timestamp: "2017-03-21T08:55:20Z",
	 *   channels: {
	 *     'thing': {
	 *       'channel': 'value'
	 *     }
	 *   }
	 * }]
	}
	 *             </pre>
	 */
	public abstract JsonObject query(ZonedDateTime fromDate, ZonedDateTime toDate, JsonObject channels, int resolution)
			throws OpenemsException;
}
