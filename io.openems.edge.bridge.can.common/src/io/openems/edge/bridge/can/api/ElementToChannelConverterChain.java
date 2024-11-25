package io.openems.edge.bridge.can.api;

public class ElementToChannelConverterChain extends ElementToChannelConverter {

	public ElementToChannelConverterChain(ElementToChannelConverter converter1, ElementToChannelConverter converter2) {
		super(
				// element -> channel
				value -> converter2.elementToChannel(converter1.elementToChannel(value)),
				// channel -> element
				value -> converter1.channelToElement(converter2.channelToElement(value)));
	}

}
