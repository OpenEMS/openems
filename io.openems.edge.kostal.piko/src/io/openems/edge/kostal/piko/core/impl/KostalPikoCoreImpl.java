package io.openems.edge.kostal.piko.core.impl;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.taskmanager.TasksManager;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.kostal.piko.charger.KostalPikoCharger;
import io.openems.edge.kostal.piko.core.api.KostalPikoCore;
import io.openems.edge.kostal.piko.ess.KostalPikoEss;
import io.openems.edge.kostal.piko.gridmeter.KostalPikoGridMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Kostal.Piko.Core", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE //
)
public class KostalPikoCoreImpl extends AbstractOpenemsComponent
		implements KostalPikoCore, OpenemsComponent, EventHandler {

	protected final static int MAX_ACTUAL_POWER = 6600;
	protected final static int MAX_APPARENT_POWER = 2300;
	private final TasksManager<ReadTask> readTasksManager;
	private SocketConnection socketConnection = null;
	private Worker worker = null;

	@Override
	public void setEss(KostalPikoEss ess) {
		this.readTasksManager.addTasks( //
				new ReadTask(ess, SymmetricEss.ChannelId.SOC, Priority.HIGH, FieldType.INTEGER, 0x02000705), //
				new ReadTask(ess, AsymmetricEss.ChannelId.ACTIVE_POWER_L1, Priority.HIGH, FieldType.FLOAT, 0x04000203), //
				new ReadTask(ess, AsymmetricEss.ChannelId.ACTIVE_POWER_L2, Priority.HIGH, FieldType.FLOAT, 0x04000303), //
				new ReadTask(ess, AsymmetricEss.ChannelId.ACTIVE_POWER_L3, Priority.HIGH, FieldType.FLOAT, 0x04000403), //
				new ReadTask(ess, SymmetricEss.ChannelId.ACTIVE_POWER, Priority.HIGH, FieldType.FLOAT, 0x04000100) //
		);
	}

	@Override
	public void unsetEss(KostalPikoEss ess) {
		this.unsetComponent(ess);
	}

	@Override
	public void setCharger(KostalPikoCharger charger) {
		this.readTasksManager.addTasks( //
				new ReadTask(charger, EssDcCharger.ChannelId.ACTUAL_POWER, Priority.HIGH, FieldType.FLOAT, 0x02000200) //
		);
	}

	@Override
	public void unsetCharger(KostalPikoCharger charger) {
		this.unsetComponent(charger);
	}

	@Override
	public void setGridMeter(KostalPikoGridMeter charger) {
		this.readTasksManager.addTasks( //
				// TODO
		);
	}

	@Override
	public void unsetGridMeter(KostalPikoGridMeter charger) {
		this.unsetComponent(charger);
	}

	private void unsetComponent(OpenemsComponent component) {
		for (ReadTask task : this.readTasksManager.getAllTasks()) {
			if (task.getComponent() == component) {
				this.readTasksManager.removeTask(task);
			}
		}
	}

	public KostalPikoCoreImpl() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
		this.readTasksManager = new TasksManager<ReadTask>(//
				/*
				 * ONCE
				 */
				// TODO: many of the following channels can change in time (like
				// HOME_CONSUMPTION_GRID)! Please check again!
				new ReadTask(this, KostalPikoCore.ChannelId.INVERTER_NAME, Priority.ONCE, FieldType.STRING, 0x01000300), //
				new ReadTask(this, KostalPikoCore.ChannelId.ARTICLE_NUMBER, Priority.ONCE, FieldType.STRING,
						0x01000100), //
				new ReadTask(this, KostalPikoCore.ChannelId.INVERTER_SERIAL_NUMBER, Priority.ONCE, FieldType.STRING,
						0x01000200), //
				new ReadTask(this, KostalPikoCore.ChannelId.FIRMWARE_VERSION, Priority.ONCE, FieldType.STRING,
						0x01000801), //
				new ReadTask(this, KostalPikoCore.ChannelId.HARDWARE_VERSION, Priority.ONCE, FieldType.STRING,
						0x01000802), //
				new ReadTask(this, KostalPikoCore.ChannelId.KOMBOARD_VERSION, Priority.ONCE, FieldType.STRING,
						0x01000803), //
				new ReadTask(this, KostalPikoCore.ChannelId.PARAMETER_VERSION, Priority.ONCE, FieldType.STRING,
						0x01000901), //
				new ReadTask(this, KostalPikoCore.ChannelId.COUNTRY_NAME, Priority.ONCE, FieldType.STRING, 0x01000902), //
				new ReadTask(this, KostalPikoCore.ChannelId.INVERTER_OPERATING_STATUS, Priority.ONCE, FieldType.STRING,
						0X08000105), //
				new ReadTask(this, KostalPikoCore.ChannelId.INVERTER_TYPE_NAME, Priority.ONCE, FieldType.STRING,
						0x01000D00), //
				new ReadTask(this, KostalPikoCore.ChannelId.NUMBER_OF_STRING, Priority.ONCE, FieldType.INTEGER,
						0x01000500), //
				new ReadTask(this, KostalPikoCore.ChannelId.NUMBER_OF_PHASES, Priority.ONCE, FieldType.INTEGER,
						0x01000600), //
				new ReadTask(this, KostalPikoCore.ChannelId.POWER_ID, Priority.ONCE, FieldType.INTEGER, 0x01000400), //
				new ReadTask(this, KostalPikoCore.ChannelId.PRESENT_ERROR_EVENT_CODE_1, Priority.ONCE,
						FieldType.INTEGER, 0x08000300), //
				new ReadTask(this, KostalPikoCore.ChannelId.PRESENT_ERROR_EVENT_CODE_2, Priority.ONCE,
						FieldType.INTEGER, 0x08000400), //
				new ReadTask(this, KostalPikoCore.ChannelId.FEED_IN_TIME, Priority.ONCE, FieldType.INTEGER, 0x0F000100), //
				new ReadTask(this, KostalPikoCore.ChannelId.INVERTER_STATUS, Priority.ONCE, FieldType.INTEGER,
						0x01000B00), //
				new ReadTask(this, KostalPikoCore.ChannelId.BAUDRATE_INDEX_MODBUS_RTU, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000206), //
				new ReadTask(this, KostalPikoCore.ChannelId.BATTERY_TEMPERATURE, Priority.ONCE, FieldType.FLOAT,
						0x02000703), //
				new ReadTask(this, KostalPikoCore.ChannelId.ISOLATION_RESISTOR, Priority.ONCE, FieldType.FLOAT,
						0x06000100), //
				new ReadTask(this, KostalPikoCore.ChannelId.GRID_FREQUENCY, Priority.ONCE, FieldType.FLOAT, 0x04000600), //
				new ReadTask(this, KostalPikoCore.ChannelId.COSINUS_PHI, Priority.ONCE, FieldType.FLOAT, 0x04000700), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_IP1, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000102), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_IP2, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000103), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_IP3, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000104), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_IP4, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000105), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_SUBNET_MASK_1, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000106), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_SUBNET_MASK_2, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000107), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_SUBNET_MASK_3, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000108), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_SUBNET_MASK_4, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000109), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_GATEWAY_1, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x0700010B), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_GATEWAY_2, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x0700010C), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_GATEWAY_3, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x0700010D), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_GATEWAY_4, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x0700010E), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_IP_DNS_FIRST_1, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x0700010F), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_IP_DNS_FIRST_2, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000110), //
				new ReadTask(this, KostalPikoCore.ChannelId.POWER_LIMITATION_OF_EVU, Priority.LOW, FieldType.FLOAT,
						0x04000500), //
				new ReadTask(this, KostalPikoCore.ChannelId.MAX_RESIDUAL_CURRENT, Priority.LOW, FieldType.FLOAT,
						0x06000301), //
				new ReadTask(this, KostalPikoCore.ChannelId.ANALOG_INPUT_CH_1, Priority.LOW, FieldType.FLOAT,
						0x0A000101), //
				new ReadTask(this, KostalPikoCore.ChannelId.ANALOG_INPUT_CH_2, Priority.LOW, FieldType.FLOAT,
						0x0A000201), //
				new ReadTask(this, KostalPikoCore.ChannelId.ANALOG_INPUT_CH_3, Priority.LOW, FieldType.FLOAT,
						0x0A000301), //
				new ReadTask(this, KostalPikoCore.ChannelId.ANALOG_INPUT_CH_4, Priority.LOW, FieldType.FLOAT,
						0x0A000401), //
				new ReadTask(this, KostalPikoCore.ChannelId.SELF_CONSUMPTION_RATE_TOTAL, Priority.LOW, FieldType.FLOAT,
						0x0F000410), //
				new ReadTask(this, KostalPikoCore.ChannelId.SELF_CONSUMPTION_RATE_DAY, Priority.LOW, FieldType.FLOAT,
						0x0F00040E), //
				new ReadTask(this, KostalPikoCore.ChannelId.DEGREE_OF_SELF_SUFFICIENCY_DAY, Priority.LOW,
						FieldType.FLOAT, 0x0F00040F), //
				new ReadTask(this, KostalPikoCore.ChannelId.DEGREE_OF_SELF_SUFFICIENCY_TOTAL, Priority.LOW,
						FieldType.FLOAT, 0x0F000411), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_POWER_L1, Priority.LOW, FieldType.FLOAT, 0x05000402), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_POWER_L2, Priority.LOW, FieldType.FLOAT, 0x05000502), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_POWER_L3, Priority.LOW, FieldType.FLOAT, 0x05000602), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_CONSUMPTION_GRID, Priority.LOW, FieldType.FLOAT,
						0x05000300), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_CONSUMPTION_PV, Priority.LOW, FieldType.FLOAT,
						0x05000100), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_TOTAL_POWER, Priority.LOW, FieldType.FLOAT,
						0x05000700), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_SELF_CONSUMPTION_TOTAL, Priority.LOW, FieldType.FLOAT,
						0x05000800), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_CONSUMPTION_L1, Priority.LOW, FieldType.FLOAT,
						0x05000403), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_CONSUMPTION_L2, Priority.LOW, FieldType.FLOAT,
						0x05000503), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_CONSUMPTION_L3, Priority.LOW, FieldType.FLOAT,
						0x05000603), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_CONSUMPTION_TOTAL, Priority.LOW, FieldType.FLOAT,
						0x0F000301), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_CONSUMPTION_DAY, Priority.LOW, FieldType.FLOAT,
						0x0F000302), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_CONSUMPTION_BATTERY, Priority.LOW, FieldType.FLOAT,
						0x05000200), //
				new ReadTask(this, KostalPikoCore.ChannelId.BATTERY_CURRENT, Priority.LOW, FieldType.FLOAT, 0x02000701), //
				new ReadTask(this, KostalPikoCore.ChannelId.SELF_CONSUMPTION_TOTAL, Priority.LOW, FieldType.FLOAT,
						0x0F000401), //
				new ReadTask(this, KostalPikoCore.ChannelId.SELF_CONSUMPTION_DAY, Priority.LOW, FieldType.FLOAT,
						0x0F000402), //
				new ReadTask(this, KostalPikoCore.ChannelId.BATTERY_VOLTAGE, Priority.LOW, FieldType.FLOAT, 0x02000702), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_IP_DNS_FIRST_3, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000111), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_IP_DNS_FIRST_4, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000112), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_IP_DNS_SECOND_1, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000113), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_IP_DNS_SECOND_2, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000114), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_IP_DNS_SECOND_3, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000115), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_IP_DNS_SECOND_4, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000116), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_AUTO_IP, Priority.ONCE, FieldType.BOOLEAN,
						0x07000101), //
				new ReadTask(this, KostalPikoCore.ChannelId.SETTING_MANUAL_EXTERNAL_ROUTER, Priority.ONCE,
						FieldType.BOOLEAN, 0x0700010A), //
				new ReadTask(this, KostalPikoCore.ChannelId.PRELOAD_MODBUS_RTU, Priority.ONCE, FieldType.BOOLEAN,
						0x07000202), //
				new ReadTask(this, KostalPikoCore.ChannelId.TERMINATION_MODBUS_RTU, Priority.ONCE, FieldType.BOOLEAN,
						0x07000203), //
				new ReadTask(this, KostalPikoCore.ChannelId.ADDRESS_MODBUS_RTU, Priority.ONCE,
						FieldType.INTEGER_UNSIGNED_BYTE, 0x07000201),

				/*
				 * LOW
				 */
				new ReadTask(this, KostalPikoCore.ChannelId.FEED_IN_STATUS, Priority.LOW, FieldType.BOOLEAN,
						0x01000A00),

				new ReadTask(this, KostalPikoCore.ChannelId.DC_VOLTAGE_STRING_1, Priority.LOW, FieldType.FLOAT,
						0x02000302), //
				new ReadTask(this, KostalPikoCore.ChannelId.DC_VOLTAGE_STRING_2, Priority.LOW, FieldType.FLOAT,
						0x02000402), //
				new ReadTask(this, KostalPikoCore.ChannelId.DC_VOLTAGE_STRING_3, Priority.LOW, FieldType.FLOAT,
						0x02000502), //

				new ReadTask(this, KostalPikoCore.ChannelId.OVERALL_DC_CURRENT, Priority.LOW, FieldType.FLOAT,
						0x02000100), //
				new ReadTask(this, KostalPikoCore.ChannelId.DC_CURRENT_STRING_1, Priority.LOW, FieldType.FLOAT,
						0x02000301), //
				new ReadTask(this, KostalPikoCore.ChannelId.DC_CURRENT_STRING_2, Priority.LOW, FieldType.FLOAT,
						0x02000401), //
				new ReadTask(this, KostalPikoCore.ChannelId.DC_CURRENT_STRING_3, Priority.LOW, FieldType.FLOAT,
						0x02000501), //

				new ReadTask(this, KostalPikoCore.ChannelId.DC_POWER_STRING_1, Priority.LOW, FieldType.FLOAT,
						0x02000303), //
				new ReadTask(this, KostalPikoCore.ChannelId.DC_POWER_STRING_2, Priority.LOW, FieldType.FLOAT,
						0x02000403), //
				new ReadTask(this, KostalPikoCore.ChannelId.DC_POWER_STRING_3, Priority.LOW, FieldType.FLOAT,
						0x02000503), //

				/*
				 * ESS
				 */
				new ReadTask(this, KostalPikoCore.ChannelId.BATTERY_CURRENT_DIRECTION, Priority.LOW, FieldType.FLOAT,
						0x02000706), //
				new ReadTask(this, KostalPikoCore.ChannelId.AC_CURRENT_L1, Priority.LOW, FieldType.FLOAT, 0x04000201), //
				new ReadTask(this, KostalPikoCore.ChannelId.AC_CURRENT_L2, Priority.LOW, FieldType.FLOAT, 0x04000301), //
				new ReadTask(this, KostalPikoCore.ChannelId.AC_CURRENT_L3, Priority.LOW, FieldType.FLOAT, 0x04000401), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_CURRENT_L1, Priority.LOW, FieldType.FLOAT, 0x05000401), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_CURRENT_L2, Priority.LOW, FieldType.FLOAT, 0x05000501), //
				new ReadTask(this, KostalPikoCore.ChannelId.HOME_CURRENT_L3, Priority.LOW, FieldType.FLOAT, 0x05000601), //
				// HIGH

				// TODO Energy Related with cycles check it again
				// new ReadTask(EssDcCharger.KostalPikoCore.ChannelId.ACTUAL_ENERGY,
				// Priority.HIGH,
				// FieldType.INTEGER_UNSIGNED_BYTE, 0x02000704),//

				new ReadTask(this, KostalPikoCore.ChannelId.AC_VOLTAGE_L1, Priority.HIGH, FieldType.FLOAT, 0x04000202), //
				new ReadTask(this, KostalPikoCore.ChannelId.AC_VOLTAGE_L2, Priority.HIGH, FieldType.FLOAT, 0x04000302), //
				new ReadTask(this, KostalPikoCore.ChannelId.AC_VOLTAGE_L3, Priority.HIGH, FieldType.FLOAT, 0x04000402), //
				new ReadTask(this, KostalPikoCore.ChannelId.YIELD_DAY, Priority.HIGH, FieldType.FLOAT, 0x0F000202), //
				new ReadTask(this, KostalPikoCore.ChannelId.YIELD_TOTAL, Priority.HIGH, FieldType.FLOAT, 0x0F000201)//
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		this.socketConnection = new SocketConnection(config.ip(), config.port(), (byte) config.unitID());
		Protocol protocol = new Protocol(socketConnection);
		this.worker = new Worker(protocol, this.readTasksManager);
		this.worker.activate(config.id());
	}

	@Deactivate
	protected void deactivate() {
		if (this.worker != null) {
			this.worker.deactivate();
		}
		if (this.socketConnection != null) {
			this.socketConnection.close();
		}
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_WRITE:
			this.worker.triggerNextCycle();
			
			
		}
	}

}
