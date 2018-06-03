package io.openems.backend.edgewebsocket.impl;

public class Attachment {

	private String apikey;
	private int[] edgeIds = {};

	public Attachment() {
	}

	public synchronized void setApikey(String apikey) {
		this.apikey = apikey;
	}

	public synchronized String getApikey() {
		return apikey;
	}

	public synchronized void setEdgeIds(int[] edgeIds) {
		this.edgeIds = edgeIds;
	}

	public synchronized int[] getEdgeIds() {
		return edgeIds;
	}

}
