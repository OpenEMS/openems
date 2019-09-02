package io.openems.edge.battery.microcare.ubmu;


import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.mccomms.IMCCommsBridge;
import io.openems.edge.bridge.mccomms.api.AbstractMCCommsComponent;
import io.openems.edge.bridge.mccomms.packet.MCCommsBitSetElement;
import io.openems.edge.bridge.mccomms.packet.MCCommsElement;
import io.openems.edge.bridge.mccomms.packet.MCCommsPacket;
import io.openems.edge.bridge.mccomms.task.ListenTask;
import io.openems.edge.bridge.mccomms.task.QueryTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.calculate.CalculateIntegerSum;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;


@Designate( ocd=Config.class, factory=true)
@Component(
		name="Battery.MCUBMU",
		immediate = true,
		configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class MCUBMU extends AbstractMCCommsComponent implements OpenemsComponent, Battery {
	
	@Reference
	protected ConfigurationAdmin cm;
	private HashSet<QueryTask> queryTasks;
	private Config config;
	private NetCurrentChannelUpdater netCurrentChannelUpdater;
	
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		BATTERY_CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)),
		BATTERY_DISCHARGE_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE));
		private final Doc doc;
		
		private ChannelId(Doc doc) {
			this.doc = doc;
		}
		
		public Doc doc() {
			return this.doc;
		}
	}
	
	public MCUBMU() {
		super(
				OpenemsComponent.ChannelId.values(),
				Battery.ChannelId.values(),
				ChannelId.values()
		);
		this.queryTasks = new HashSet<>();
		this.netCurrentChannelUpdater = new NetCurrentChannelUpdater();
		channel(ChannelId.BATTERY_DISCHARGE_CURRENT).onSetNextValue(value -> netCurrentChannelUpdater.dischargeCurrentChannelUpdated());
		channel(ChannelId.BATTERY_CHARGE_CURRENT).onSetNextValue(value -> netCurrentChannelUpdater.chargeCurrentChannelUpdated());
	}
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	public void setMCCommsBridge(IMCCommsBridge bridge){
		super.setMCCommsBridge(bridge);
	}
	
	
	@Activate
	public void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.UBMUmcCommsAddress(), cm, config.mcCommsBridge_id());
		this.config = config;
		try {
			//noinspection unchecked
			queryTasks.add(
					constructQueryTask(
							56,
							57,
							MCCommsElement.newInstanceFromChannel(7, 2, channel(Battery.ChannelId.SOC))
									.setScaleFactor(0.01),
							MCCommsElement.newInstanceFromChannel(9, 2, channel(Battery.ChannelId.SOH))
									.setScaleFactor(0.01),
							MCCommsElement.newInstanceFromChannel(11, 2, channel(Battery.ChannelId.VOLTAGE))
									.setScaleFactor(0.001),
							MCCommsElement.newInstanceFromChannel(13, 2, channel(ChannelId.BATTERY_CHARGE_CURRENT))
									.setScaleFactor(100),
							MCCommsElement.newInstanceFromChannel(15, 2, channel(ChannelId.BATTERY_DISCHARGE_CURRENT))
									.setScaleFactor(100),
							MCCommsBitSetElement.newInstanceFromChannels(18, 2, channel(Battery.ChannelId.READY_FOR_WORKING))
					).queryRepeatedly(config.RTDrefreshMS(), TimeUnit.MILLISECONDS)
			);
			queryTasks.add(
					constructQueryTask(
							58,
							59,
							MCCommsElement.newInstanceFromChannel(7, 2, channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE))
									.setScaleFactor(0.001),
							MCCommsElement.newInstanceFromChannel(9, 2, channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE))
									.setScaleFactor(0.001),
							MCCommsElement.newInstanceFromChannel(11, 2, channel(Battery.ChannelId.CHARGE_MAX_CURRENT))
									.setScaleFactor(0.1),
							MCCommsElement.newInstanceFromChannel(13, 2, channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT))
									.setScaleFactor(0.1)
					).queryRepeatedly(config.RTDrefreshMS(), TimeUnit.MILLISECONDS)
			);
			queryTasks.add(
					constructQueryTask(
							60,
							61,
							MCCommsElement.newInstanceFromChannel(7, 2, channel(Battery.ChannelId.MAX_CELL_VOLTAGE)),
							MCCommsElement.newInstanceFromChannel(11, 2, channel(Battery.ChannelId.MIN_CELL_VOLTAGE)),
							MCCommsElement.newInstanceFromChannel(17, 1, channel(Battery.ChannelId.MAX_CELL_TEMPERATURE))
									.setUnsigned(false),
							MCCommsElement.newInstanceFromChannel(19, 1, channel(Battery.ChannelId.MIN_CELL_TEMPERATURE))
									.setUnsigned(false)
					).queryRepeatedly(config.statusRefreshMS(), TimeUnit.MILLISECONDS)
			);
			queryTasks.add(
					constructQueryTask(
							64,
							65,
							MCCommsElement.newInstanceFromChannel(19, 2, channel(Battery.ChannelId.CAPACITY))
									.setScaleFactor(100)
					).queryRepeatedly(config.statusRefreshMS(), TimeUnit.MILLISECONDS)
			);
		} catch (OpenemsException e) {
			logError(logger, e.getMessage());
		}
	}
	
	private QueryTask constructQueryTask(int queryCommand, int responseCommand, MCCommsElement...responsePacketElements) throws OpenemsException {
		//noinspection unchecked
		return QueryTask.newCommandOnlyQuery(
				getMCCommsBridge(),
				config.openemsMCCommsAddress(),
				config.UBMUmcCommsAddress(),
				queryCommand,
				config.queryTimeoutMS(),
				TimeUnit.MILLISECONDS,
				new ListenTask(
						config.UBMUmcCommsAddress(),
						config.openemsMCCommsAddress(),
						responseCommand,
						new MCCommsPacket(responsePacketElements)
				)
		);
	}

	@Deactivate
	protected void deactivate() {
		for(QueryTask queryTask : queryTasks) {
			queryTask.cancel();
		}
		super.deactivate();
	}
	
	private class NetCurrentChannelUpdater {
		private boolean chargeCurrentChannelUpdated;
		private boolean dischargeCurrentChannelUpdated;
		
		NetCurrentChannelUpdater() {
			chargeCurrentChannelUpdated = false;
			dischargeCurrentChannelUpdated = false;
		}
		
		void dischargeCurrentChannelUpdated() {
			dischargeCurrentChannelUpdated = true;
			tryUpdateNetCurrentChannelValue();
		}
		
		void chargeCurrentChannelUpdated() {
			chargeCurrentChannelUpdated = true;
			tryUpdateNetCurrentChannelValue();
		}
		
		private void tryUpdateNetCurrentChannelValue() {
			if (chargeCurrentChannelUpdated && dischargeCurrentChannelUpdated) {
				chargeCurrentChannelUpdated = false;
				dischargeCurrentChannelUpdated = false;
				CalculateIntegerSum sum = new CalculateIntegerSum();
				sum.addValue(channel(ChannelId.BATTERY_DISCHARGE_CURRENT));
				sum.addValue(channel(ChannelId.BATTERY_CHARGE_CURRENT));
				channel(Battery.ChannelId.CURRENT).setNextValue(sum.calculate());
			}
		}
	}
}
