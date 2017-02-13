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

import java.util.Optional;

import io.openems.api.thing.Thing;

public class FunctionalChannel<T> extends ReadChannel<T> implements ChannelUpdateListener {

	private ReadChannel<T>[] channels;
	private FunctionalChannelFunction<T> func;

	public FunctionalChannel(String id, Thing parent, FunctionalChannelFunction<T> function,
			ReadChannel<T>... channels) {
		super(id, parent);
		this.channels = channels;
		this.func = function;
		for (Channel c : channels) {
			c.addUpdateListener(this);
		}
	}

	@Override
	public void channelUpdated(Channel channel, Optional<?> newValue) {
		updateValue(func.handle(channels));
	}

	@Override
	public FunctionalChannel<T> label(T value, String label) {
		super.label(value, label);
		return this;
	}
}
