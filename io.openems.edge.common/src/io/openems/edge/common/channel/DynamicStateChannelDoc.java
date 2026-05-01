package io.openems.edge.common.channel;

import java.util.function.Consumer;

import io.openems.common.channel.Level;
import io.openems.edge.common.channel.dynamicdoctext.DynamicDocText;
import io.openems.edge.common.component.OpenemsComponent;

/**
 * This class extends the default StateChannelDoc. It adds the feature to
 * provide different channelDoc data depending on the component channel. For
 * example, this can be a different text depending on channel states.
 */
public class DynamicStateChannelDoc extends StateChannelDoc implements DynamicChannelDoc<Boolean> {
	private final Consumer<DynamicStateChannelDoc> factory;

	private DynamicStateChannelDoc(Level level, Consumer<DynamicStateChannelDoc> factory) {
		super(level);
		this.factory = factory;
		this.factory.accept(this);
	}

	@Override
	public void setDynamicDocText(DynamicDocText dynamicDocText) {
		this.getTextFunction = dynamicDocText::getText;
	}

	private DynamicStateChannelDoc createDocCopy() {
		return new DynamicStateChannelDoc(this.getLevel(), this.factory);
	}

	@SuppressWarnings("unchecked")
	@Override
	public StateChannel createChannelInstance(OpenemsComponent component,
			io.openems.edge.common.channel.ChannelId channelId) {
		var channelDoc = this.createDocCopy();
		return super.createChannelInstance(component, channelId, channelDoc);
	}

	/**
	 * Starts the builder to create a {@link DynamicStateChannelDoc}.
	 * 
	 * @param level Level for {@link StateChannelDoc}
	 * @return Builder instance
	 */
	public static DynamicChannelDocBuilder<DynamicStateChannelDoc> builder(Level level) {
		return new DynamicChannelDocBuilder<>(x -> new DynamicStateChannelDoc(level, x));
	}
}
