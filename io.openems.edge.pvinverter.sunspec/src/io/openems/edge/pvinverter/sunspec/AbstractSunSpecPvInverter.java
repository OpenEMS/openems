package io.openems.edge.pvinverter.sunspec;

import java.util.Map;
import java.util.Optional;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;

public abstract class AbstractSunSpecPvInverter extends AbstractOpenemsSunSpecComponent
		implements SunSpecPvInverter, ManagedSymmetricPvInverter, SymmetricMeter, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(AbstractSunSpecPvInverter.class);
	private final SetPvLimitHandler setPvLimitHandler = new SetPvLimitHandler(this);

	public AbstractSunSpecPvInverter(Map<SunSpecModel, Priority> activeModels,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) throws OpenemsException {
		super(activeModels, firstInitialChannelIds, furtherInitialChannelIds);
		this._setActiveConsumptionEnergy(0);
	}

	/**
	 * Make sure to call this method from the inheriting OSGi Component.
	 *
	 * @throws OpenemsException on error
	 */
	@Override
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId, int readFromCommonBlockNo)
			throws OpenemsException {
		return super.activate(context, id, alias, enabled, unitId, cm, modbusReference, modbusId,
				readFromCommonBlockNo);
	}

	/**
	 * Make sure to call this method from the inheriting OSGi Component.
	 */
	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Make sure to call this method from the inheriting OSGi Component.
	 *
	 * <p>
	 * Requires:
	 *
	 * <pre>
	 * property = { //
	 *   EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
	 * }
	 * </pre>
	 *
	 */
	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled() || !this.isSunSpecInitializationCompleted()) {
			this.channel(SunSpecPvInverter.ChannelId.PV_LIMIT_FAILED).setNextValue(false);
			return;
		}

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			try {
				this.setPvLimitHandler.run();

				this.channel(SunSpecPvInverter.ChannelId.PV_LIMIT_FAILED).setNextValue(false);
			} catch (OpenemsNamedException e) {
				this.channel(SunSpecPvInverter.ChannelId.PV_LIMIT_FAILED).setNextValue(true);
			}
			break;
		}
	}

	@Override
	public MeterType getMeterType() {
		return MeterType.PRODUCTION;
	}

	@Override
	public String debugLog() {
		return "L:" + this.getActivePower().asString();
	}

	@Override
	protected void onSunSpecInitializationCompleted() {
		this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");

		/*
		 * SymmetricMeter
		 */
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.FREQUENCY, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				DefaultSunSpecModel.S111.HZ, DefaultSunSpecModel.S112.HZ, DefaultSunSpecModel.S113.HZ,
				DefaultSunSpecModel.S101.HZ, DefaultSunSpecModel.S102.HZ, DefaultSunSpecModel.S103.HZ);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.ACTIVE_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S111.W, DefaultSunSpecModel.S112.W, DefaultSunSpecModel.S113.W,
				DefaultSunSpecModel.S101.W, DefaultSunSpecModel.S102.W, DefaultSunSpecModel.S103.W);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.REACTIVE_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S111.V_AR, DefaultSunSpecModel.S112.V_AR, DefaultSunSpecModel.S113.V_AR,
				DefaultSunSpecModel.S101.V_AR, DefaultSunSpecModel.S102.V_AR, DefaultSunSpecModel.S103.V_AR);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S111.WH, DefaultSunSpecModel.S112.WH, DefaultSunSpecModel.S113.WH,
				DefaultSunSpecModel.S101.WH, DefaultSunSpecModel.S102.WH, DefaultSunSpecModel.S103.WH);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.VOLTAGE, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				DefaultSunSpecModel.S111.PH_VPH_A, DefaultSunSpecModel.S111.PH_VPH_B, DefaultSunSpecModel.S111.PH_VPH_C,
				DefaultSunSpecModel.S112.PH_VPH_A, DefaultSunSpecModel.S112.PH_VPH_B, DefaultSunSpecModel.S112.PH_VPH_C,
				DefaultSunSpecModel.S113.PH_VPH_A, DefaultSunSpecModel.S113.PH_VPH_B, DefaultSunSpecModel.S113.PH_VPH_C,
				DefaultSunSpecModel.S101.PH_VPH_A, DefaultSunSpecModel.S101.PH_VPH_B, DefaultSunSpecModel.S101.PH_VPH_C,
				DefaultSunSpecModel.S102.PH_VPH_A, DefaultSunSpecModel.S102.PH_VPH_B, DefaultSunSpecModel.S102.PH_VPH_C,
				DefaultSunSpecModel.S103.PH_VPH_A, DefaultSunSpecModel.S103.PH_VPH_B,
				DefaultSunSpecModel.S103.PH_VPH_C);
		this.mapFirstPointToChannel(//
				SymmetricMeter.ChannelId.CURRENT, //
				ElementToChannelConverter.SCALE_FACTOR_3, //
				DefaultSunSpecModel.S111.A, DefaultSunSpecModel.S112.A, DefaultSunSpecModel.S113.A,
				DefaultSunSpecModel.S101.A, DefaultSunSpecModel.S102.A, DefaultSunSpecModel.S103.A);
		this.mapFirstPointToChannel(//
				ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S120.W_RTG);
	}

	@Override
	protected <T extends Channel<?>> Optional<T> getSunSpecChannel(SunSpecPoint point) {
		return super.getSunSpecChannel(point);
	}

	@Override
	protected <T extends Channel<?>> T getSunSpecChannelOrError(SunSpecPoint point) throws OpenemsException {
		return super.getSunSpecChannelOrError(point);
	}
}
