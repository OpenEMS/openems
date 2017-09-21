package io.openems.impl.device.custom.riedmann;

import io.openems.api.channel.ReadChannel;
import io.openems.api.channel.WriteChannel;
import io.openems.api.device.nature.DeviceNature;
import io.openems.api.doc.ThingInfo;

@ThingInfo(title = "Custom: Riedmann PLC")
public interface RiedmannNature extends DeviceNature {

	public ReadChannel<Long> getWaterlevel();

	public ReadChannel<Long> getGetPivotOn();

	public ReadChannel<Long> getBorehole1On();

	public ReadChannel<Long> getBorehole2On();

	public ReadChannel<Long> getBorehole3On();

	public ReadChannel<Long> getClima1On();

	public ReadChannel<Long> getClima2On();

	public ReadChannel<Long> getOfficeOn();

	public ReadChannel<Long> getTraineeCenterOn();

	public ReadChannel<Long> getAutomaticMode();

	public ReadChannel<Long> getManualMode();

	public ReadChannel<Long> getEmergencyStop();

	public ReadChannel<Long> getSwitchStatePivotPump();

	public ReadChannel<Long> getSwitchStatePivotDrive();

	public ReadChannel<Long> getError();

	public ReadChannel<Long> getWaterLevelBorehole1On();

	public ReadChannel<Long> getWaterLevelBorehole1Off();

	public ReadChannel<Long> getWaterLevelBorehole2On();

	public ReadChannel<Long> getWaterLevelBorehole2Off();

	public ReadChannel<Long> getWaterLevelBorehole3On();

	public ReadChannel<Long> getWaterLevelBorehole3Off();

	public WriteChannel<Long> getSetPivotOn();

	public WriteChannel<Long> getSetBorehole1On();

	public WriteChannel<Long> getSetBorehole2On();

	public WriteChannel<Long> getSetBorehole3On();

	public WriteChannel<Long> getSetClima1On();

	public WriteChannel<Long> getSetClima2On();

	public WriteChannel<Long> getSetOfficeOn();

	public WriteChannel<Long> getSetTraineeCenterOn();

	public WriteChannel<Long> getSignalBus1On();

	public WriteChannel<Long> getSignalBus2On();

	public WriteChannel<Long> getSignalGridOn();

	public WriteChannel<Long> getSignalSystemStop();

	public WriteChannel<Long> getSignalWatchdog();

	public WriteChannel<Long> getSetWaterLevelBorehole1On();

	public WriteChannel<Long> getSetWaterLevelBorehole1Off();

	public WriteChannel<Long> getSetWaterLevelBorehole2On();

	public WriteChannel<Long> getSetWaterLevelBorehole2Off();

	public WriteChannel<Long> getSetWaterLevelBorehole3On();

	public WriteChannel<Long> getSetWaterLevelBorehole3Off();

}
