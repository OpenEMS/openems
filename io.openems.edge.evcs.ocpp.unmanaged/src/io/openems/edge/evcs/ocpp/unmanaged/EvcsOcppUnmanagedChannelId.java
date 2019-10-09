package io.openems.edge.evcs.ocpp.unmanaged;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public enum EvcsOcppUnmanagedChannelId implements io.openems.edge.common.channel.ChannelId{

	/*
	 * Session Id.
	 * Id is set if there is a new Session between - the EVCS implemented by this Component and the Server.
	 * If this value is empty, no communication was established
	 */
	CHARGING_SESSION_ID(Doc.of(OpenemsType.STRING).text("Identifies a current Session set by the server")), //


	
	
	
	/*
	 * SampledValue 
	 */
	
	
		
	//TODO: Put this in the EVCS NATURE
	/*
	 * Fail State Channels
	 */
	CHARGINGSTATION_COMMUNICATION_FAILED(Doc.of(Level.FAULT));

	private final Doc doc;

	private EvcsOcppUnmanagedChannelId(Doc doc) {
		this.doc = doc;
	}
	
	@Override
	public Doc doc() {
		return this.doc;
	}
}
