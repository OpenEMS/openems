/*
 * OpenEMS - Open Source Energy Management System
 * Copyright (c) 2016 FENECON GmbH & Co. KG
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

 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package io.openems.impl.protocol.modbus.internal;

public class Range {
	private Element[] elements;
	private final int length;
	private final int startAddress;

	public Range(int startAddress, Element... elements) {
		this.startAddress = startAddress;
		this.elements = elements;
		for (Element element : elements) {
			element.setRange(this);
		}
		int length = 0;
		for (Element element : elements) {
			length += element.getLength();
		}
		this.length = length;
	}

	public Element[] getElements() {
		return elements;
	}

	public int getStartAddress() {
		return startAddress;
	}

	@Override
	public String toString() {
		return "Range [startAddress=" + startAddress + ", length=" + length + "]";
	}
}
