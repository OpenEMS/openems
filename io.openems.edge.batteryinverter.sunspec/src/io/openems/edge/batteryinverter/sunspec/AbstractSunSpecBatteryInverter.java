package io.openems.edge.batteryinverter.sunspec;

import java.util.Map;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.ISunSpecModel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;

public abstract class AbstractSunSpecBatteryInverter extends AbstractOpenemsSunSpecComponent
		implements ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractSunSpecBatteryInverter.class);

	public AbstractSunSpecBatteryInverter(Map<ISunSpecModel, Priority> activeModels,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) {
		super(activeModels, firstInitialChannelIds, furtherInitialChannelIds);
	}

	/**
	 * Make sure to call this method from the inheriting OSGi Component.
	 */
	@Override
	protected boolean activate(ComponentContext context, String id, String alias, boolean enabled, int unitId,
			ConfigurationAdmin cm, String modbusReference, String modbusId, int readFromCommonBlockNo) {
		return super.activate(context, id, alias, enabled, unitId, cm, modbusReference, modbusId,
				readFromCommonBlockNo);
	}

	/**
	 * Make sure to call this method from the inheriting OSGi Component.
	 */
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected void onSunSpecInitializationCompleted() {
		this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");
//
//		/*
//		 * SymmetricMeter
//		 */
//		this.mapFirstPointToChannel(//
//				SymmetricMeter.ChannelId.FREQUENCY, //
//				ElementToChannelConverter.SCALE_FACTOR_3, //
//				SunSpecModel.S111.HZ, SunSpecModel.S112.HZ, SunSpecModel.S113.HZ, SunSpecModel.S101.HZ,
//				SunSpecModel.S102.HZ, SunSpecModel.S103.HZ);
//		this.mapFirstPointToChannel(//
//				SymmetricMeter.ChannelId.ACTIVE_POWER, //
//				ElementToChannelConverter.DIRECT_1_TO_1, //
//				SunSpecModel.S111.W, SunSpecModel.S112.W, SunSpecModel.S113.W, SunSpecModel.S101.W, SunSpecModel.S102.W,
//				SunSpecModel.S103.W);
//		this.mapFirstPointToChannel(//
//				SymmetricMeter.ChannelId.REACTIVE_POWER, //
//				ElementToChannelConverter.DIRECT_1_TO_1, //
//				SunSpecModel.S111.V_AR, SunSpecModel.S112.V_AR, SunSpecModel.S113.V_AR, SunSpecModel.S101.V_AR,
//				SunSpecModel.S102.V_AR, SunSpecModel.S103.V_AR);
//		this.mapFirstPointToChannel(//
//				SymmetricMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY, //
//				ElementToChannelConverter.DIRECT_1_TO_1, //
//				SunSpecModel.S111.WH, SunSpecModel.S112.WH, SunSpecModel.S113.WH, SunSpecModel.S101.WH,
//				SunSpecModel.S102.WH, SunSpecModel.S103.WH);
//		this.mapFirstPointToChannel(//
//				SymmetricMeter.ChannelId.VOLTAGE, //
//				ElementToChannelConverter.SCALE_FACTOR_3, //
//				SunSpecModel.S111.PH_VPH_A, SunSpecModel.S111.PH_VPH_B, SunSpecModel.S111.PH_VPH_C,
//				SunSpecModel.S112.PH_VPH_A, SunSpecModel.S112.PH_VPH_B, SunSpecModel.S112.PH_VPH_C,
//				SunSpecModel.S113.PH_VPH_A, SunSpecModel.S113.PH_VPH_B, SunSpecModel.S113.PH_VPH_C,
//				SunSpecModel.S101.PH_VPH_A, SunSpecModel.S101.PH_VPH_B, SunSpecModel.S101.PH_VPH_C,
//				SunSpecModel.S102.PH_VPH_A, SunSpecModel.S102.PH_VPH_B, SunSpecModel.S102.PH_VPH_C,
//				SunSpecModel.S103.PH_VPH_A, SunSpecModel.S103.PH_VPH_B, SunSpecModel.S103.PH_VPH_C);
//		this.mapFirstPointToChannel(//
//				SymmetricMeter.ChannelId.CURRENT, //
//				ElementToChannelConverter.SCALE_FACTOR_3, //
//				SunSpecModel.S111.A, SunSpecModel.S112.A, SunSpecModel.S113.A, SunSpecModel.S101.A, SunSpecModel.S102.A,
//				SunSpecModel.S103.A);
//		this.mapFirstPointToChannel(//
//				ManagedSymmetricPvInverter.ChannelId.MAX_APPARENT_POWER, //
//				ElementToChannelConverter.DIRECT_1_TO_1, //
//				SunSpecModel.S120.W_RTG);
	}
}
