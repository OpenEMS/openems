package io.openems.edge.battery.microcare.ubmu;


import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.bridge.mccomms.IMCCommsBridge;
import io.openems.edge.bridge.mccomms.api.AbstractMCCommsComponent;
import io.openems.edge.bridge.mccomms.packet.MCCommsElement;
import io.openems.edge.bridge.mccomms.packet.MCCommsPacket;
import io.openems.edge.bridge.mccomms.task.ListenTask;
import io.openems.edge.bridge.mccomms.task.QueryTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.*;
import org.osgi.service.metatype.annotations.Designate;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;


@SuppressWarnings("PackageAccessibility") //IntelliJ inspection warning suppression
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
	
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
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
	}
	
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	public void setMCCommsBridge(IMCCommsBridge bridge){
		super.setMCCommsBridge(bridge);
	}
	
	
	@Activate
	public void activate(ComponentContext context, Config config) {
		System.out.println("MCUBMU Activate called"); //TODO remove debug
		logError(logger, "activate called");
		super.activate(context, config.id(), config.alias(), config.enabled(), config.UBMUmcCommsAddress(), cm, config.mcCommsBridge_id());
		try {
			queryTasks.add(
					QueryTask.newCommandOnlyQuery(
							getMCCommsBridge(),
							config.openemsMCCommsAddress(),
							config.UBMUmcCommsAddress(),
							180,
							config.queryTimeoutMS(),
							TimeUnit.MILLISECONDS,
							new ListenTask(
									config.UBMUmcCommsAddress(),
									config.openemsMCCommsAddress(),
									181,
									new MCCommsPacket(
											MCCommsElement.newInstanceFromChannel(7, 1, channel(Battery.ChannelId.MAX_CELL_TEMPERATURE)),
											MCCommsElement.newInstanceFromChannel(8, 4, channel(Battery.ChannelId.CAPACITY)),
											MCCommsElement.newInstanceFromChannel(12, 1, channel(Battery.ChannelId.READY_FOR_WORKING)),
											MCCommsElement.newInstanceFromChannel(13, 1, channel(Battery.ChannelId.SOC)),
											MCCommsElement.newInstanceFromChannel(14, 1, channel(Battery.ChannelId.SOH)),
											MCCommsElement.newInstanceFromChannel(15, 2, channel(Battery.ChannelId.CURRENT)),
											MCCommsElement.newInstanceFromChannel(17, 2, channel(Battery.ChannelId.VOLTAGE), 0.001)
									)
							)
					).queryRepeatedly(config.RTDrefreshMS(), TimeUnit.MILLISECONDS)
			);
			queryTasks.add(
					QueryTask.newCommandOnlyQuery(
							getMCCommsBridge(),
							config.openemsMCCommsAddress(),
							config.UBMUmcCommsAddress(),
							182,
							config.queryTimeoutMS(),
							TimeUnit.MILLISECONDS,
							new ListenTask(
									config.UBMUmcCommsAddress(),
									config.openemsMCCommsAddress(),
									183,
									new MCCommsPacket(
											MCCommsElement.newInstanceFromChannel(7, 2, channel(Battery.ChannelId.CHARGE_MAX_CURRENT), 0.1),
											MCCommsElement.newInstanceFromChannel(9, 2, channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE), 0.1),
											MCCommsElement.newInstanceFromChannel(11, 2, channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT), 0.1),
											MCCommsElement.newInstanceFromChannel(13, 2, channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE), 0.1),
											MCCommsElement.newInstanceFromChannel(15, 2, channel(Battery.ChannelId.MAX_CELL_VOLTAGE)),
											MCCommsElement.newInstanceFromChannel(17, 2, channel(Battery.ChannelId.MIN_CELL_VOLTAGE)),
											MCCommsElement.newInstanceFromChannel(19,1, channel(Battery.ChannelId.MAX_CELL_TEMPERATURE)),
											MCCommsElement.newInstanceFromChannel(20, 1, channel(Battery.ChannelId.MIN_CELL_TEMPERATURE))
									)
							)
					).queryRepeatedly(config.statusRefreshMS(), TimeUnit.MILLISECONDS)
			);
		} catch (OpenemsException e) {
			logError(logger, e.getMessage());
		}
	}

	@Deactivate
	protected void deactivate() {
		for(QueryTask queryTask : queryTasks) {
			queryTask.cancel();
		}
		super.deactivate();
	}

}
