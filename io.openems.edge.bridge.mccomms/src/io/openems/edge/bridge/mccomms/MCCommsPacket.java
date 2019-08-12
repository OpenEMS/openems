package io.openems.edge.bridge.mccomms;

import com.google.common.collect.ImmutableRangeMap;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.mccomms.Element.MCCommsElement;

import java.util.Map;
import java.util.Optional;

public class MCCommsPacket {
	private final RangeMap<Integer, MCCommsElement> elements;
	
	public MCCommsPacket(MCCommsElement ... elements) {
		ImmutableRangeMap.Builder<Integer, MCCommsElement> builder = ImmutableRangeMap.builder();
		for (MCCommsElement element : elements) {
			builder.put(element.getAddressRange(), element);
		}
		this.elements = builder.build();
	}
	
	private byte getByte(int whichByte) {
		return Optional.ofNullable(elements.get(whichByte)).map(e -> e.getBytes()[whichByte - e.getAddressRange().lowerEndpoint()]).orElse(((byte) 170));
	}
	
	public byte[] getBytes() {
		byte[] returnBytes = new byte[elements.span().upperEndpoint()];
		for (int i = 0; i < elements.span().upperEndpoint(); i++) {
			returnBytes[i] = getByte(i);
		}
		return returnBytes;
	}
	
	public MCCommsPacket setBytes(byte[] bytes) {
		for (MCCommsElement value : elements.asMapOfRanges().values()) {
			
		}
	}
}
