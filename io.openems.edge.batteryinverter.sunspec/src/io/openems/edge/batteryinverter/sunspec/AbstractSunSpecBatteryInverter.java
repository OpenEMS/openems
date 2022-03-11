package io.openems.edge.batteryinverter.sunspec;

import java.util.Map;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.bridge.modbus.sunspec.AbstractOpenemsSunSpecComponent;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;

public abstract class AbstractSunSpecBatteryInverter extends AbstractOpenemsSunSpecComponent
		implements ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AbstractSunSpecBatteryInverter.class);

	public AbstractSunSpecBatteryInverter(Map<SunSpecModel, Priority> activeModels,
			io.openems.edge.common.channel.ChannelId[] firstInitialChannelIds,
			io.openems.edge.common.channel.ChannelId[]... furtherInitialChannelIds) throws OpenemsException {
		super(activeModels, firstInitialChannelIds, furtherInitialChannelIds);
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

	@Override
	protected void onSunSpecInitializationCompleted() {
		this.logInfo(this.log, "SunSpec initialization finished. " + this.channels().size() + " Channels available.");
	}
}
