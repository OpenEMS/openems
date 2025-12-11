package io.openems.edge.phoenixcontact.plcnext.gds;

import io.openems.edge.phoenixcontact.plcnext.PlcNextDevice;

public record PlcNextGdsDataProviderConfig(String dataUrl, String dataInstanceName, PlcNextDevice device) {

	public static final String PLC_NEXT_OPENEMS_COMPONENT_NAME = "OpenEMS_V1Component1";

}
