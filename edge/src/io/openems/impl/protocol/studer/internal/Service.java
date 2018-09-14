package io.openems.impl.protocol.studer.internal;

import java.util.ArrayList;
import java.util.List;

import io.openems.impl.protocol.studer.internal.object.ObjectType;
import io.openems.impl.protocol.studer.internal.object.PropertyId;

public abstract class Service {

	protected boolean isResponse;
	protected boolean isError;

	protected boolean isDatalogSupported;

	protected boolean isNewDataloggerFilePresent;

	protected boolean isSdCardFull;

	protected boolean isSdCardPresent;

	protected boolean isRccRestart;

	protected boolean isMessagePending;

	protected int srcAddress;

	protected int dstAddress;

	protected ObjectType objectType;

	protected PropertyId propertyId;

	protected int objectId;

	protected static List<Byte> calculateChecksum(List<Byte> bytes) {
		int a = (byte) 0xFF;
		int b = (byte) 0x00;
		for (int value : bytes) {
			a = (a + value) % 0x100;
			b = (b + a) % 0x100;
		}
		List<Byte> erg = new ArrayList<>();
		erg.add((byte) a);
		erg.add((byte) b);
		return erg;
	}

}
