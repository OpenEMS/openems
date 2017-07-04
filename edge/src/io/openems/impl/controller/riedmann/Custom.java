package io.openems.impl.controller.riedmann;

import io.openems.api.channel.WriteChannel;
import io.openems.api.controller.IsThingMap;
import io.openems.api.controller.ThingMap;
import io.openems.impl.device.custom.riedmann.RiedmannNature;

@IsThingMap(type = RiedmannNature.class)
public class Custom extends ThingMap {

	public WriteChannel<Long> watchdog;
	public WriteChannel<Long> supplyBus1On;
	public WriteChannel<Long> supplyBus2On;
	public WriteChannel<Long> setPivotOn;
	public WriteChannel<Long> setBorehole1On;
	public WriteChannel<Long> setBorehole2On;
	public WriteChannel<Long> setBorehole3On;
	public WriteChannel<Long> setClima1On;
	public WriteChannel<Long> setClima2On;
	public WriteChannel<Long> setOfficeOn;
	public WriteChannel<Long> setTraineeCenterOn;
	public WriteChannel<Long> signalGridOn;
	public WriteChannel<Long> signalSystemStop;
	public WriteChannel<Long> setWaterLevelBorehole1On;
	public WriteChannel<Long> setWaterLevelBorehole1Off;
	public WriteChannel<Long> setWaterLevelBorehole2On;
	public WriteChannel<Long> setWaterLevelBorehole2Off;
	public WriteChannel<Long> setWaterLevelBorehole3On;
	public WriteChannel<Long> setWaterLevelBorehole3Off;

	public Custom(RiedmannNature thing) {
		super(thing);
		this.watchdog = thing.getSignalWatchdog().required();
		this.supplyBus1On = thing.getSignalBus1On().required();
		this.supplyBus2On = thing.getSignalBus2On().required();
		this.setPivotOn = thing.getSetPivotOn().required();
		this.setBorehole1On = thing.getSetBorehole1On().required();
		this.setBorehole2On = thing.getSetBorehole2On().required();
		this.setBorehole3On = thing.getSetBorehole3On().required();
		this.setClima1On = thing.getSetClima1On().required();
		this.setClima2On = thing.getSetClima2On().required();
		this.setOfficeOn = thing.getSetOfficeOn().required();
		this.setTraineeCenterOn = thing.getSetTraineeCenterOn().required();
		this.signalGridOn = thing.getSignalGridOn().required();
		this.signalSystemStop = thing.getSignalSystemStop().required();
		this.setWaterLevelBorehole1On = thing.getSetWaterLevelBorehole1On().required();
		this.setWaterLevelBorehole1Off = thing.getSetWaterLevelBorehole1Off().required();
		this.setWaterLevelBorehole2On = thing.getSetWaterLevelBorehole2On().required();
		this.setWaterLevelBorehole2Off = thing.getSetWaterLevelBorehole2Off().required();
		this.setWaterLevelBorehole3On = thing.getSetWaterLevelBorehole3On().required();
		this.setWaterLevelBorehole3Off = thing.getSetWaterLevelBorehole3Off().required();
	}

}
