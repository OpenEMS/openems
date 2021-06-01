package io.openems.edge.battery.bmw.cooling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.bmw.BmwBatteryImpl;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "BmwBatteryCoolingUnit", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
		} //
)
public class BmwBatteryCoolingUnit extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

	private final int TWO_POINT_CONTROLLER_UPPER_THRESHOLD_degC = 33;
	private final int TWO_POINT_CONTROLLER_LOWER_THRESHOLD_degC = 32;
	private final int DEFAULT_MAX_CELL_TEMPERATURE_degC = 0;

	private Config config = null;
	private BooleanWriteChannel digitalOutputCoolingEnableChannel;

	String[] essIds;
	@Reference
	private ComponentManager manager;

	protected List<BmwBatteryImpl> getBatteryList() throws OpenemsNamedException {
		List<BmwBatteryImpl> batteryList = new ArrayList<BmwBatteryImpl>();
		for (String batteryId : this.config.batteryIds()) {
			if (this.id().equals("")) {
				continue;
			}
			BmwBatteryImpl battery = this.manager.getComponent(batteryId);
			if (battery instanceof BmwBatteryImpl) {
				batteryList.add((BmwBatteryImpl) battery);
			}
		}
		return batteryList;
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		ACTIVE(Doc.of(OpenemsType.BOOLEAN).unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
		RUNNING_STATUS(Doc.of(RunningStatus.values()).accessMode(AccessMode.READ_WRITE)), //
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public BmwBatteryCoolingUnit() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws IllegalArgumentException, OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.digitalOutputCoolingEnableChannel = manager.getChannel(ChannelAddress.fromString(config.coolingCommand()));
		digitalOutputCoolingEnableChannel.setNextWriteValue(false);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		this.channel(ChannelId.ACTIVE).setNextValue(this.isEnabled());
		if (!this.isEnabled()) {
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			try {
				this.doBatteryCooling(this.getBatteryList());
			} catch (Exception e) {
				this.channel(ChannelId.ACTIVE).setNextValue(false);
			}
			break;
		}
	}

	private void doBatteryCooling(List<BmwBatteryImpl> batteryList) throws Exception {
		List<Integer> maxTemperatures = new ArrayList<Integer>();

		// --- read Status of Digital-Output Cooling-Enable ---
		if (this.digitalOutputCoolingEnableChannel.value().orElse(false)) {
			this.setBatteryCoolingRunningStatus(RunningStatus.ON);
		} else {
			this.setBatteryCoolingRunningStatus(RunningStatus.OFF);
		}

		// --- read maximum Temperature ---
		for (BmwBatteryImpl bmwBatteryImpl : batteryList) {
			if (bmwBatteryImpl.isStarted()) {
				maxTemperatures.add(bmwBatteryImpl.getMaxCellTemperature().orElse(0));
			} else {
				maxTemperatures.add(DEFAULT_MAX_CELL_TEMPERATURE_degC);
			}
		}

		// --- execute two-point Controller ---
		if ((Collections.max(maxTemperatures) >= TWO_POINT_CONTROLLER_UPPER_THRESHOLD_degC)
				&& (this.getBatteryCoolingRunningStatus() == RunningStatus.OFF)) {
			digitalOutputCoolingEnableChannel.setNextWriteValue(true);
		} else if ((Collections.max(maxTemperatures) <= TWO_POINT_CONTROLLER_LOWER_THRESHOLD_degC)
				&& (this.getBatteryCoolingRunningStatus() == RunningStatus.ON)) {
			digitalOutputCoolingEnableChannel.setNextWriteValue(false);
		}
	}

	private RunningStatus getBatteryCoolingRunningStatus() {
		EnumReadChannel runningStatusChannel = this.channel(ChannelId.RUNNING_STATUS);
		RunningStatus runningStatus = runningStatusChannel.value().asEnum();
		return runningStatus;
	}

	private void setBatteryCoolingRunningStatus(RunningStatus runningStatus) {
		this.channel(ChannelId.RUNNING_STATUS).setNextValue(runningStatus);
	}

	@Override
	public String debugLog() {
		return " | Active: " + this.channel(ChannelId.ACTIVE).value().asString() //
				+ " | Running Status: " + this.channel(ChannelId.RUNNING_STATUS).value().asOptionString() //
		;
	}
}
