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
package io.openems.impl.controller.debuglog;

import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.api.device.nature.io.OutputNature;

@IsThingMap(type = OutputNature.class)
public class Output extends ThingMap {

	private final OutputNature output;

	public Output(OutputNature output) {
		super(output);
		this.output = output;
	}

	@Override
	public String toString() {
		StringBuilder b = new StringBuilder();
		b.append(output.id() + " [");
		int i = 0;
		for (WriteChannel<Boolean> channel : output.setOutput()) {
			String value;
			if (channel.valueOptional().isPresent()) {
				value = channel.valueOptional().get() ? "x" : "-";
			} else {
				value = "?";
			}
			b.append(channel.id() + value);

			// add space for all but the last
			if (++i < output.setOutput().length) {
				b.append(" ");
			}
		}
		b.append("]");
		return b.toString();
	}
}
