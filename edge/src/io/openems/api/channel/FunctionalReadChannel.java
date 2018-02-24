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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import io.openems.api.exception.InvalidValueException;
import io.openems.api.thing.Thing;

public class FunctionalReadChannel<T> extends ReadChannel<T> implements ChannelUpdateListener {

	private List<ReadChannel<T>> channels = new ArrayList<>();
	private FunctionalReadChannelFunction<T> func;

	@SafeVarargs
	public FunctionalReadChannel(String id, Thing parent, FunctionalReadChannelFunction<T> function,
			ReadChannel<T>... channels) {
		super(id, parent);
		this.channels.addAll(Arrays.asList(channels));
		this.func = function;
		for (Channel c : channels) {
			c.addUpdateListener(this);
		}
	}

	public FunctionalReadChannel(String id, Thing parent, FunctionalReadChannelFunction<T> function) {
		super(id, parent);
		this.func = function;
		for (Channel c : channels) {
			c.addUpdateListener(this);
		}
	}

	public void addChannel(ReadChannel<T> channel) {
		synchronized (channels) {
			this.channels.add(channel);
			channel.addUpdateListener(this);
			update();
		}
	}

	public void removeChannel(ReadChannel<T> channel) {
		synchronized (this.channels) {
			channel.removeUpdateListener(this);
			this.channels.remove(channel);
			update();
		}
	}

	@Override
	public void channelUpdated(Channel channel, Optional<?> newValue) {
		update();
	}

	private void update() {
		synchronized (this.channels) {
			@SuppressWarnings("unchecked") ReadChannel<T>[] channels = new ReadChannel[this.channels.size()];
			this.channels.toArray(channels);
			try {
				updateValue(func.handle(channels));
			} catch (InvalidValueException e) {
				updateValue(null);
			}
		}
	}

	@Override
	public FunctionalReadChannel<T> label(T value, String label) {
		super.label(value, label);
		return this;
	}

	@Override
	public FunctionalReadChannel<T> unit(String unit) {
		super.unit(unit);
		return this;
	}
}
