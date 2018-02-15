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
package io.openems.api.channel;

import io.openems.api.channel.thingstate.ThingStateEnum;
import io.openems.api.thing.Thing;

public class StaticThingStateChannel extends ThingStateChannel {

	public StaticThingStateChannel(ThingStateEnum state, Thing parent, boolean value) {
		super(state, parent);
		this.updateValue(value);
	}

	@Override public StaticThingStateChannel addUpdateListener(ChannelUpdateListener... listeners) {
		super.addUpdateListener(listeners);
		return this;
	}

	@Override public StaticThingStateChannel addChangeListener(ChannelChangeListener... listeners) {
		super.addChangeListener(listeners);
		return this;
	}

	public void setValue(boolean b) {
		this.updateValue(b);
	}

}
