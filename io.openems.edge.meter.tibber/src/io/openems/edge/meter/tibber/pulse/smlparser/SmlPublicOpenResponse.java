package io.openems.edge.meter.tibber.pulse.smlparser;

public class SmlPublicOpenResponse extends SmlMessageBody {

	protected byte[] codepage;
	protected byte[] clientId;
	protected byte[] reqFileId;
	protected byte[] serverId;
	protected SmlTime refTime;
	protected Integer smlVersion;

	public byte[] getCodepage() {
		return this.codepage;
	}

	public void setCodepage(byte[] codepage) {
		this.codepage = codepage;
	}

	public byte[] getClientId() {
		return this.clientId;
	}

	public void setClientId(byte[] clientId) {
		this.clientId = clientId;
	}

	public byte[] getReqFileId() {
		return this.reqFileId;
	}

	public void setReqFileId(byte[] reqFileId) {
		this.reqFileId = reqFileId;
	}

	public byte[] getServerId() {
		return this.serverId;
	}

	public void setServerId(byte[] serverId) {
		this.serverId = serverId;
	}

	public SmlTime getRefTime() {
		return this.refTime;
	}

	public void setRefTime(SmlTime refTime) {
		this.refTime = refTime;
	}

	public Integer getSmlVersion() {
		return this.smlVersion;
	}

	public void setSmlVersion(Integer smlVersion) {
		this.smlVersion = smlVersion;
	}

}
