package io.openems.edge.ess.fronius.gen24;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public enum FroniusGen24ChannelId implements io.openems.edge.common.channel.ChannelId {

    SERIAL_NUMBER(Doc.of(OpenemsType.LONG)), //
    CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
	    .unit(Unit.WATT) //
    ), DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
	    .unit(Unit.WATT) //
    ),
    /**
     * Requested Active Power.
     * 
     * <ul>
     * <li>Interface: Ess Symmetric
     * <li>Type: Integer
     * <li>Unit: W
     * <li>Range: negative values for Charge; positive for Discharge
     * </ul>
     */
    REQUESTED_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
	    .unit(Unit.WATT) //
    ) //
    ;

    private final Doc doc;

    private FroniusGen24ChannelId(Doc doc) {
	this.doc = doc;
    }

    @Override
    public Doc doc() {
	return this.doc;
    }

}