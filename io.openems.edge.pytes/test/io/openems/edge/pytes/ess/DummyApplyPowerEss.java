package io.openems.edge.pytes.ess;

import org.osgi.service.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractDummyOpenemsComponent;
import io.openems.edge.common.test.TestUtils;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.pytes.battery.PytesBattery;
import io.openems.edge.pytes.dccharger.PytesDcCharger;
import io.openems.edge.pytes.enums.WorkState;

/**
 * Fake ESS for {@link ApplyPowerHandlerTest}: channels are real, the values
 * the handler needs are plain fields.
 */
public class DummyApplyPowerEss extends AbstractDummyOpenemsComponent<DummyApplyPowerEss> implements ApplyPowerEss {

	private final Logger log = LoggerFactory.getLogger(DummyApplyPowerEss.class);

	private int cycleTime = 1000;
	private int batteryDischargeLimit = 2100;
	private int batteryChargeLimit = -2100;
	private Integer gridFeedInLimit = null;
	private int failsafeMinutes = 5;
	private Integer gridPower = null;
	private boolean pvLimitActive = false;

	public DummyApplyPowerEss(String id) {
		super(id, //
				OpenemsComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				HybridEss.ChannelId.values(), //
				PytesJs3.ChannelId.values() //
		);
		TestUtils.withValue(this, PytesJs3.ChannelId.WORK_STATE, WorkState.NORMAL);
	}

	@Override
	protected DummyApplyPowerEss self() {
		return this;
	}

	// ---- values the handler reads via channels ----

	DummyApplyPowerEss withActivePower(int value) {
		TestUtils.withValue(this, SymmetricEss.ChannelId.ACTIVE_POWER, value);
		return this;
	}

	DummyApplyPowerEss withDcDischargePower(int value) {
		TestUtils.withValue(this, HybridEss.ChannelId.DC_DISCHARGE_POWER, value);
		return this;
	}

	DummyApplyPowerEss withMaxApparentPower(int value) {
		TestUtils.withValue(this, SymmetricEss.ChannelId.MAX_APPARENT_POWER, value);
		return this;
	}

	DummyApplyPowerEss withAllowedChargePower(int value) {
		TestUtils.withValue(this, ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER, value);
		return this;
	}

	DummyApplyPowerEss withAllowedDischargePower(int value) {
		TestUtils.withValue(this, ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER, value);
		return this;
	}

	DummyApplyPowerEss withWorkState(WorkState value) {
		TestUtils.withValue(this, PytesJs3.ChannelId.WORK_STATE, value);
		return this;
	}

	// ---- values the handler reads via ApplyPowerEss ----

	DummyApplyPowerEss withCycleTime(int ms) {
		this.cycleTime = ms;
		return this;
	}

	DummyApplyPowerEss withBatteryLimits(int chargeLimit, int dischargeLimit) {
		this.batteryChargeLimit = chargeLimit;
		this.batteryDischargeLimit = dischargeLimit;
		return this;
	}

	DummyApplyPowerEss withGridFeedInLimit(Integer limit) {
		this.gridFeedInLimit = limit;
		return this;
	}

	DummyApplyPowerEss withFailsafeMinutes(int minutes) {
		this.failsafeMinutes = minutes;
		return this;
	}

	@Override
	public Logger getLogger() {
		return this.log;
	}

	@Override
	public int getCycleTime() {
		return this.cycleTime;
	}

	@Override
	public int getBatteryDischargeLimit() {
		return this.batteryDischargeLimit;
	}

	@Override
	public int getBatteryChargeLimit() {
		return this.batteryChargeLimit;
	}

	@Override
	public Integer getGridFeedInLimit() {
		return this.gridFeedInLimit;
	}

	DummyApplyPowerEss withGridPower(Integer value) {
		this.gridPower = value;
		return this;
	}

	DummyApplyPowerEss withPvLimitActive(boolean value) {
		this.pvLimitActive = value;
		return this;
	}

	@Override
	public Integer getGridPower() {
		return this.gridPower;
	}

	@Override
	public boolean isPvLimitActive() {
		return this.pvLimitActive;
	}

	@Override
	public int getFailsafeMinutes() {
		return this.failsafeMinutes;
	}

	@Override
	public void debugLog(String message) {
		// silent
	}

	// ---- nature methods not used by the handler ----

	@Override
	public Power getPower() {
		return null;
	}

	@Override
	public int getPowerPrecision() {
		return 10;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) {
		// not used
	}

	@Override
	public Integer getSurplusPower() {
		return null;
	}

	@Override
	public void handleEvent(Event event) {
		// not used
	}

	@Override
	public void addBattery(PytesBattery battery) {
		// not used
	}

	@Override
	public void removeBattery(PytesBattery battery) {
		// not used
	}

	@Override
	public void addCharger(PytesDcCharger charger) {
		// not used
	}

	@Override
	public void removeCharger(PytesDcCharger charger) {
		// not used
	}

	@Override
	public String getModbusBridgeId() {
		return "modbus0";
	}

	@Override
	public Integer getUnitId() {
		return 1;
	}
}
