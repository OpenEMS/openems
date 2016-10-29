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
package io.openems.api.channel.numeric;

import java.util.HashSet;
import java.util.Set;

import io.openems.api.channel.ChannelBuilder;

public class NumericChannelBuilder<B extends NumericChannelBuilder<?>> extends ChannelBuilder<B> {
	public enum Aggregation {
		SUM
	}

	protected Set<NumericChannel> channels = null;
	protected Aggregation aggregate = null;

	@Override
	public NumericChannel build() {
		if (channels != null) {
			if (aggregate == Aggregation.SUM) {
				return new SumNumericChannel(channelId, nature, unit, minValue, maxValue, multiplier, delta, labels,
						channels);
			}
		}
		return new NumericChannel(channelId, nature, unit, minValue, maxValue, multiplier, delta, labels);
	}

	@SuppressWarnings("unchecked")
	public B channel(NumericChannel... channels) {
		this.channels = new HashSet<>();
		for (NumericChannel channel : channels) {
			this.channels.add(channel);
		}
		return (B) this;
	}

	@SuppressWarnings("unchecked")
	public B aggregate(Aggregation aggregate) {
		this.aggregate = aggregate;
		return (B) this;
	}

}
