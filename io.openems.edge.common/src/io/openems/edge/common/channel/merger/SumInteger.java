package io.openems.edge.common.channel.merger;

import io.openems.edge.common.channel.doc.ChannelId;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

import java.util.NoSuchElementException;

public class SumInteger<C extends OpenemsComponent> extends ChannelsFunction<C, Integer> {

	public SumInteger(OpenemsComponent parent, ChannelId targetChannelId, ChannelId sourceChannelId) {
		super(parent, targetChannelId, sourceChannelId);
	}

	protected double calculate() throws NoSuchElementException {
		return this.valueMap.values() //
				.stream() //
				.filter(v -> v.asOptional().isPresent()) //
				.mapToDouble(Value::get) //
				.sum();
	}
	
//	protected boolean isDoubleCompatible() {
//		
//	}
}
