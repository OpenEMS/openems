package io.openems.impl.controller.riedmann;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ConfigInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;

@ThingInfo(title = "Sps parameter Controller")
public class RiedmannController extends Controller implements ChannelChangeListener {

	/*
	 * Config-Channel
	 */
	@ConfigInfo(title = "Waterlevel Borehole 1 On", description = "This configuration sets the waterlevel to start Borehole Pump 1", type = Long.class)
	public ConfigChannel<Long> setWaterLevelBorehole1On = new ConfigChannel<Long>("wl1On", this).defaultValue(50L);
	@ConfigInfo(title = "Waterlevel Borehole 1 Off", description = "This configuration sets the waterlevel to stop Borehole Pump 1", type = Long.class)
	public ConfigChannel<Long> setWaterLevelBorehole1Off = new ConfigChannel<Long>("wl1Off", this).defaultValue(100L);
	@ConfigInfo(title = "Waterlevel Borehole 2 On", description = "This configuration sets the waterlevel to start Borehole Pump 2", type = Long.class)
	public ConfigChannel<Long> setWaterLevelBorehole2On = new ConfigChannel<Long>("wl2On", this).defaultValue(200L);
	@ConfigInfo(title = "Waterlevel Borehole 2 Off", description = "This configuration sets the waterlevel to stop Borehole Pump 2", type = Long.class)
	public ConfigChannel<Long> setWaterLevelBorehole2Off = new ConfigChannel<Long>("wl2Off", this).defaultValue(300L);
	@ConfigInfo(title = "Waterlevel Borehole 3 On", description = "This configuration sets the waterlevel to start Borehole Pump 3", type = Long.class)
	public ConfigChannel<Long> setWaterLevelBorehole3On = new ConfigChannel<Long>("wl3On", this).defaultValue(400L);
	@ConfigInfo(title = "Waterlevel Borehole 3 Off", description = "This configuration sets the waterlevel to stop Borehole Pump 3", type = Long.class)
	public ConfigChannel<Long> setWaterLevelBorehole3Off = new ConfigChannel<Long>("wl3Off", this).defaultValue(500L);
	@ConfigInfo(title = "Soc Hysteresis", description = "hysteresis for the switching of the loads.", type = Long.class)
	public ConfigChannel<Long> socHysteresis = new ConfigChannel<Long>("socHysteresis", this).defaultValue(10L);
	@ConfigInfo(title = "Soc Load 1 Off", description = "Below this Soc the Load 1 will be disconnected.", type = Long.class)
	public ConfigChannel<Long> socLoad1Off = new ConfigChannel<>("socLoad1Off", this);
	@ConfigInfo(title = "Soc Load 2 Off", description = "Below this Soc the Load 2 will be disconnected.", type = Long.class)
	public ConfigChannel<Long> socLoad2Off = new ConfigChannel<>("socLoad2Off", this);
	@ConfigInfo(title = "Soc Load 3 Off", description = "Below this Soc the Load 3 will be disconnected.", type = Long.class)
	public ConfigChannel<Long> socLoad3Off = new ConfigChannel<>("socLoad3Off", this);
	@ConfigInfo(title = "Soc Load 4 Off", description = "Below this Soc the Load 4 will be disconnected.", type = Long.class)
	public ConfigChannel<Long> socLoad4Off = new ConfigChannel<>("socLoad4Off", this);

	@ConfigInfo(title = "SPS", description = "The sps which should be controlled.", type = Custom.class)
	public ConfigChannel<Custom> sps = new ConfigChannel<>("sps", this);
	@ConfigInfo(title = "ESS", description = "The ess to stop on system stop. Also used for Off-Grid indication for the SPS. ", type = Ess.class)
	public ConfigChannel<Ess> ess = new ConfigChannel<>("ess", this);

	/*
	 * Attributes
	 */
	private boolean watchdogState = false;
	private boolean updateWaterLevelBorehole1On = false;
	private boolean updateWaterLevelBorehole1Off = false;
	private boolean updateWaterLevelBorehole2On = false;
	private boolean updateWaterLevelBorehole2Off = false;
	private boolean updateWaterLevelBorehole3On = false;
	private boolean updateWaterLevelBorehole3Off = false;
	private boolean load1On = true;
	private boolean load2On = true;
	private boolean load3On = true;
	private boolean load4On = true;

	public RiedmannController() {
		super();
	}

	public RiedmannController(String thingId) {
		super(thingId);
	}

	@Override
	public void run() {
		try {
			Ess ess = this.ess.value();
			Custom sps = this.sps.value();
			// Watchdog
			try {
				if (watchdogState) {
					sps.watchdog.pushWrite(0L);
					watchdogState = false;
				} else {
					sps.watchdog.pushWrite(1L);
					watchdogState = true;
				}
			} catch (WriteChannelException e) {
				log.error("Failed to set Watchdog!", e);
			}
			// Water level
			if (updateWaterLevelBorehole1Off) {
				try {
					sps.setWaterLevelBorehole1Off.pushWrite(setWaterLevelBorehole1Off.value());
					updateWaterLevelBorehole1Off = false;
				} catch (InvalidValueException | WriteChannelException e) {
					log.error("Failed to set WaterLevelBorehole1Off!", e);
				}
			}
			if (updateWaterLevelBorehole1On) {
				try {
					sps.setWaterLevelBorehole1On.pushWrite(setWaterLevelBorehole1On.value());
					updateWaterLevelBorehole1On = false;
				} catch (InvalidValueException | WriteChannelException e) {
					log.error("Failed to set WaterLevelBorehole1On!", e);
				}
			}
			if (updateWaterLevelBorehole2Off) {
				try {
					sps.setWaterLevelBorehole2Off.pushWrite(setWaterLevelBorehole2Off.value());
					updateWaterLevelBorehole2Off = false;
				} catch (InvalidValueException | WriteChannelException e) {
					log.error("Failed to set WaterLevelBorehole2Off!", e);
				}
			}
			if (updateWaterLevelBorehole2On) {
				try {
					sps.setWaterLevelBorehole2On.pushWrite(setWaterLevelBorehole2On.value());
					updateWaterLevelBorehole2On = false;
				} catch (InvalidValueException | WriteChannelException e) {
					log.error("Failed to set WaterLevelBorehole2On!", e);
				}
			}
			if (updateWaterLevelBorehole3Off) {
				try {
					sps.setWaterLevelBorehole3Off.pushWrite(setWaterLevelBorehole3Off.value());
					updateWaterLevelBorehole3Off = false;
				} catch (InvalidValueException | WriteChannelException e) {
					log.error("Failed to set WaterLevelBorehole3Off!", e);
				}
			}
			if (updateWaterLevelBorehole3On) {
				try {
					sps.setWaterLevelBorehole3On.pushWrite(setWaterLevelBorehole3On.value());
					updateWaterLevelBorehole3On = false;
				} catch (InvalidValueException | WriteChannelException e) {
					log.error("Failed to set WaterLevelBorehole3On!", e);
				}
			}
			// Load switching
			try {
				if (ess.soc.value() >= socLoad1Off.value() + socHysteresis.value()
						|| ess.gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
					load1On = true;
				} else if (ess.soc.value() <= socLoad1Off.value()) {
					load1On = false;
				}
				if (load1On) {
					sps.setClima1On.pushWrite(1L);
					sps.setClima2On.pushWrite(1L);
				} else {
					sps.setClima1On.pushWrite(0L);
					sps.setClima2On.pushWrite(0L);
				}
			} catch (WriteChannelException e) {
				log.error("Failed to connect/disconnect Load 1", e);
			}
			try {
				if (ess.soc.value() >= socLoad2Off.value() + socHysteresis.value()
						|| ess.gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
					load2On = true;
				} else if (ess.soc.value() <= socLoad2Off.value()) {
					load2On = false;
				}
				if (load2On) {
					sps.setPivotOn.pushWrite(1L);
				} else {
					sps.setPivotOn.pushWrite(0L);
				}
			} catch (WriteChannelException e) {
				log.error("Failed to connect/disconnect Load 2", e);
			}
			try {
				if (ess.soc.value() >= socLoad3Off.value() + socHysteresis.value()
						|| ess.gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
					load3On = true;
				} else if (ess.soc.value() <= socLoad3Off.value()) {
					load3On = false;
				}
				if (load3On) {
					sps.setBorehole1On.pushWrite(1L);
					sps.setBorehole2On.pushWrite(1L);
					sps.setBorehole3On.pushWrite(1L);
				} else {
					sps.setBorehole1On.pushWrite(0L);
					sps.setBorehole2On.pushWrite(0L);
					sps.setBorehole3On.pushWrite(0L);
				}
			} catch (WriteChannelException e) {
				log.error("Failed to connect/disconnect Load 3", e);
			}
			try {
				if (ess.soc.value() >= socLoad4Off.value() + socHysteresis.value()
						|| ess.gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
					load4On = true;
				} else if (ess.soc.value() <= socLoad4Off.value()) {
					load4On = false;
				}
				if (load4On) {
					sps.setOfficeOn.pushWrite(1L);
					sps.setTraineeCenterOn.pushWrite(1L);
				} else {
					sps.setOfficeOn.pushWrite(0L);
					sps.setTraineeCenterOn.pushWrite(0L);
				}
			} catch (WriteChannelException e) {
				log.error("Failed to connect/disconnect Load 4", e);
			}
		} catch (InvalidValueException e) {
			log.error("Can't read value!", e);
		}
	}

	@Override
	public void channelChanged(Channel channel, Optional<?> newValue, Optional<?> oldValue) {
		if (channel.equals(setWaterLevelBorehole1Off)) {
			updateWaterLevelBorehole1Off = true;
		} else if (channel.equals(setWaterLevelBorehole1On)) {
			updateWaterLevelBorehole1On = true;
		} else if (channel.equals(setWaterLevelBorehole2Off)) {
			updateWaterLevelBorehole2Off = true;
		} else if (channel.equals(setWaterLevelBorehole2On)) {
			updateWaterLevelBorehole2On = true;
		} else if (channel.equals(setWaterLevelBorehole3Off)) {
			updateWaterLevelBorehole3Off = true;
		} else if (channel.equals(setWaterLevelBorehole3On)) {
			updateWaterLevelBorehole3On = true;
		}
	}
}
