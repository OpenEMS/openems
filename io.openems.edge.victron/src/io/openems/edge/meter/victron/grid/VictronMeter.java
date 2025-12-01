package io.openems.edge.meter.victron.grid;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.meter.api.ElectricityMeter;

public interface VictronMeter extends ElectricityMeter {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
	SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
	);

	private final Doc doc;

	private ChannelId(Doc doc) {
	    this.doc = doc;
	}

	@Override
	public Doc doc() {
	    return this.doc;
	}
    }

}
