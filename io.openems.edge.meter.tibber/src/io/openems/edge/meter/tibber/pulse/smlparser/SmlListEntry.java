package io.openems.edge.meter.tibber.pulse.smlparser;

import org.openmuc.jsml.EUnit;

public class SmlListEntry extends AbstractSmlObject {
	
	protected byte[] objName;
	protected Long status;
	protected SmlTime valTime;
	protected EUnit valUnit;
	protected Number scaler;
	protected Object value;
	protected byte[] valueSignature;
	
	public byte[] getObjName() {
		return this.objName;
	}
	
	public Long getStatus() {
		return this.status;
	}
	
	public SmlTime getValTime() {
		return this.valTime;
	}
	
	public EUnit getValUnit() {
		return this.valUnit;
	}
	
	public Number getScaler() {
		return this.scaler;
	}
	
	public Object getValue() {
		return this.value;
	}
	
	public byte[] getValueSignature() {
		return this.valueSignature;
	}
	
	

}
