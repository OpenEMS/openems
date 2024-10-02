package io.openems.edge.batteryinverter.sunspec;

import java.util.Map;
import java.util.Optional;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecPoint;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

public abstract class AbstractSunSpecDcCharger extends AbstractOpenemsSunSpecComponent
		// TODO Implement HybridEss, ManagedSymmetricEss, AsymmetricEss
		implements SunSpecDcCharger, EssDcCharger, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractSunSpecBatteryInverter.class);

	public AbstractSunSpecDcCharger(Map<SunSpecModel, Priority> activeModels,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) throws OpenemsException {
		super(activeModels, firstInitialChannelIds, furtherInitialChannelIds);
		// this._setGridMode(GridMode.ON_GRID);
	}

	/**
	 * Make sure to call this method from the inheriting OSGi Component.
	 *
	 * @param context               ComponentContext of this component. Receive it
	 *                              from parameter for @Activate
	 * @param id                    ID of this component. Typically 'config.id()'
	 * @param alias                 Human-readable name of this Component. Typically
	 *                              'config.alias()'. Defaults to 'id' if empty
	 * @param enabled               Whether the component should be enabled.
	 *                              Typically 'config.enabled()'
	 * @param unitId                Unit-ID of the Modbus target
	 * @param cm                    An instance of ConfigurationAdmin. Receive it
	 *                              using @Reference
	 * @param modbusReference       The name of the @Reference setter method for the
	 *                              Modbus bridge - e.g. 'Modbus' if you have a
	 *                              setModbus()-method
	 * @param modbusId              The ID of the Modbus bridge. Typically
	 *                              'config.modbus_id()'
	 * @param readFromCommonBlockNo the starting block number
	 * @return true if the target filter was updated. You may use it to abort the
	 *         activate() method.
	 * @throws OpenemsException on error
	 */
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

	@Override
	public String debugLog() {
		return new StringBuilder() //

				.append("|Charger ActualPower:").append(this.getActualPower().asString()) //

				.toString();
	}

	@Override
	protected void onSunSpecInitializationCompleted() {
		this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");

		// this.mapFirstPointToChannel(//
		// SymmetricEss.ChannelId.SOC, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S802.SO_C);
		//
		// this.mapFirstPointToChannel(//
		// SymmetricEss.ChannelId.CAPACITY, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S802.W_H_RTG);
		//
		// // TODO this is the battery power; wrong for hybrid system
		// this.mapFirstPointToChannel(//
		// SymmetricEss.ChannelId.ACTIVE_POWER, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S802.W);

		// TODO Channels: REACTIVE_POWER, MAX_APPARENT_POWER, ACTIVE_CHARGE_ENERGY,
		// ACTIVE_DISCHARGE_ENERGY,...
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
