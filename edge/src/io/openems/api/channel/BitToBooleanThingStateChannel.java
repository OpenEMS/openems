package io.openems.api.channel;

import io.openems.api.channel.thingstate.ThingStateEnum;
import io.openems.api.thing.Thing;

public class BitToBooleanThingStateChannel extends ThingStateChannel implements ChannelChangeListener{

	public BitToBooleanThingStateChannel(ThingStateEnum state, Thing parent) {
		super(state, parent);
		// TODO Auto-generated constructor stub
	}
	// MOVED TO OSGi
	//	private ReadChannel<? extends Number> valueChannel;
	//	private int bitIndex;
	//
	//	public BitToBooleanThingStateChannel(ThingStateEnum state, Thing parent, ReadChannel<? extends Number> channel, int bitIndex){
	//		super(state, parent);
	//		this.valueChannel = channel;
	//		this.valueChannel.addChangeListener(this);
	//		this.bitIndex = bitIndex;
	//	}
	//
	//	@Override
	//	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
	//		if(valueChannel.isValuePresent()) {
	//			if(valueChannel.getValue().longValue() << ~bitIndex < 0) {
	//				updateValue(true);
	//			}else {
	//				updateValue(false);
	//			}
	//		}
	//	}

}
