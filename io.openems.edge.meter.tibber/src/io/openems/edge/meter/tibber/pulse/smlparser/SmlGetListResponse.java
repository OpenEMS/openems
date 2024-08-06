package io.openems.edge.meter.tibber.pulse.smlparser;

import java.util.List;

public class SmlGetListResponse extends SmlMessageBody {
	
	protected byte[] clientId;
	protected byte[] serverId;
	protected byte[] listName;
	protected SmlTime actSensorTime;
	protected List<SmlListEntry> valList;
	protected byte[] listSignature;
	protected SmlTime actGatewayTime;
	
	public byte[] getClientId() {
		return this.clientId;
	}
	
	public byte[] getServerId() {
		return this.serverId;
	}
	
	public byte[] getListName() {
		return this.listName;
	}
	
	public SmlTime getActSensorTime() {
		return this.actSensorTime;
	}
	
	public List<SmlListEntry> getValList() {
		return this.valList;
	}
	
	public byte[] getListSignature() {
		return this.listSignature;
	}
	
	public SmlTime getActGatewayTime() {
		return this.actGatewayTime;
	}
	
	
}
