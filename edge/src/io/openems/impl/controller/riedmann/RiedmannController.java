package io.openems.impl.controller.riedmann;

import java.util.Optional;

import io.openems.api.channel.Channel;
import io.openems.api.channel.ChannelChangeListener;
import io.openems.api.channel.ConfigChannel;
import io.openems.api.controller.Controller;
import io.openems.api.device.nature.ess.EssNature;
import io.openems.api.doc.ChannelInfo;
import io.openems.api.doc.ThingInfo;
import io.openems.api.exception.InvalidValueException;
import io.openems.api.exception.WriteChannelException;
import io.openems.impl.controller.symmetric.timelinecharge.Ess;

@ThingInfo(title = "Sps parameter Controller")
public class RiedmannController extends Controller implements ChannelChangeListener {

	/*
	 * Config-Channel
	 */

	@ChannelInfo(title = "Waterlevel Borehole 1 On", description = "This configuration sets the waterlevel to start Borehole Pump 1", type = Long.class)
	public ConfigChannel<Long> setWaterLevelBorehole1On = new ConfigChannel<Long>("wl1On", this).defaultValue(50L);
	@ChannelInfo(title = "Waterlevel Borehole 1 Off", description = "This configuration sets the waterlevel to stop Borehole Pump 1", type = Long.class)
	public ConfigChannel<Long> setWaterLevelBorehole1Off = new ConfigChannel<Long>("wl1Off", this).defaultValue(100L);
	@ChannelInfo(title = "Waterlevel Borehole 2 On", description = "This configuration sets the waterlevel to start Borehole Pump 2", type = Long.class)
	public ConfigChannel<Long> setWaterLevelBorehole2On = new ConfigChannel<Long>("wl2On", this).defaultValue(200L);
	@ChannelInfo(title = "Waterlevel Borehole 2 Off", description = "This configuration sets the waterlevel to stop Borehole Pump 2", type = Long.class)
	public ConfigChannel<Long> setWaterLevelBorehole2Off = new ConfigChannel<Long>("wl2Off", this).defaultValue(300L);
	@ChannelInfo(title = "Waterlevel Borehole 3 On", description = "This configuration sets the waterlevel to start Borehole Pump 3", type = Long.class)
	public ConfigChannel<Long> setWaterLevelBorehole3On = new ConfigChannel<Long>("wl3On", this).defaultValue(400L);
	@ChannelInfo(title = "Waterlevel Borehole 3 Off", description = "This configuration sets the waterlevel to stop Borehole Pump 3", type = Long.class)
	public ConfigChannel<Long> setWaterLevelBorehole3Off = new ConfigChannel<Long>("wl3Off", this).defaultValue(500L);
	@ChannelInfo(title = "Soc Hysteresis", description = "hysteresis for the switching of the loads.", type = Long.class)
	public ConfigChannel<Long> socHysteresis = new ConfigChannel<Long>("socHysteresis", this).defaultValue(10L);
	@ChannelInfo(title = "Soc Load 1 Off", description = "Below this Soc the Load 1 will be disconnected.", type = Long.class)
	public ConfigChannel<Long> socLoad1Off = new ConfigChannel<>("socLoad1Off", this);
	@ChannelInfo(title = "Soc Load 2 Off", description = "Below this Soc the Load 2 will be disconnected.", type = Long.class)
	public ConfigChannel<Long> socLoad2Off = new ConfigChannel<>("socLoad2Off", this);
	@ChannelInfo(title = "Soc Load 3 Off", description = "Below this Soc the Load 3 will be disconnected.", type = Long.class)
	public ConfigChannel<Long> socLoad3Off = new ConfigChannel<>("socLoad3Off", this);
	@ChannelInfo(title = "Soc Load 4 Off", description = "Below this Soc the Load 4 will be disconnected.", type = Long.class)
	public ConfigChannel<Long> socLoad4Off = new ConfigChannel<>("socLoad4Off", this);

	@ChannelInfo(title = "SPS", description = "The sps which should be controlled.", type = Custom.class)
	public ConfigChannel<Custom> sps = new ConfigChannel<>("sps", this);
	@ChannelInfo(title = "ESS", description = "The ess to stop on system stop. Also used for Off-Grid indication for the SPS. ", type = Ess.class)
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
		// Check if all parameters are available
		Ess ess;
		Custom sps;
		try {
			ess = this.ess.value();
			sps = this.sps.value();
		} catch (InvalidValueException | NullPointerException e) {
			log.error("TimelineChargeController error: " + e.getMessage());
			return;
		}
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
			log.error("Failed to set Watchdog: " + e.getMessage());
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
		/*
		 * Load switching
		 */
		// Check if all parameters are available
		long essSoc;
		long socHysteresis;
		long socLoad1Off;
		long socLoad2Off;
		long socLoad3Off;
		long socLoad4Off;
		try {
			essSoc = ess.soc.value();
			socHysteresis = this.socHysteresis.value();
			socLoad1Off = this.socLoad1Off.value();
			socLoad2Off = this.socLoad2Off.value();
			socLoad3Off = this.socLoad3Off.value();
			socLoad4Off = this.socLoad4Off.value();
		} catch (InvalidValueException | NullPointerException e) {
			log.error("TimelineChargeController error: " + e.getMessage());
			return;
		}
		try {
			if (essSoc >= socLoad1Off + socHysteresis
					|| ess.gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
				load1On = true;
			} else if (essSoc <= socLoad1Off) {
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
			log.error("Failed to connect/disconnect Load 1: " + e.getMessage());
		}
		try {
			if (essSoc >= socLoad2Off + socHysteresis
					|| ess.gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
				load2On = true;
			} else if (essSoc <= socLoad2Off) {
				load2On = false;
			}
			if (load2On) {
				sps.setPivotOn.pushWrite(1L);
			} else {
				sps.setPivotOn.pushWrite(0L);
			}
		} catch (WriteChannelException e) {
			log.error("Failed to connect/disconnect Load 2: " + e.getMessage());
		}
		try {
			if (essSoc >= socLoad3Off + socHysteresis
					|| ess.gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
				load3On = true;
			} else if (essSoc <= socLoad3Off) {
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
			log.error("Failed to connect/disconnect Load 3: " + e.getMessage());
		}
		try {
			if (essSoc >= socLoad4Off + socHysteresis
					|| ess.gridMode.labelOptional().equals(Optional.of(EssNature.ON_GRID))) {
				load4On = true;
			} else if (essSoc <= socLoad4Off) {
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
			log.error("Failed to connect/disconnect Load 4: " + e.getMessage());
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
