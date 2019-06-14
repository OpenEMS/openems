package io.openems.edge.bridge.mbus.api;

public class DataRequest {
	
	protected String mbus;
	protected int primaryAddress;
	
	public DataRequest(String mbus, int primaryAddress) {
		this.mbus = mbus;
		this.primaryAddress = primaryAddress;
	}

}
