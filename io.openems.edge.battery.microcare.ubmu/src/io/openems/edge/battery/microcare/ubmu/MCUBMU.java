package io.openems.edge.battery.microcare.ubmu;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;

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

/**
 * Component factory class for interfacing with the Microcare Universal Battery
 * Management Unit (MCUBMU), currently in development
 */
@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Battery.Microcare.UBMU", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class MCUBMU extends AbstractMCCommsComponent implements OpenemsComponent, Battery {

	@Reference
	protected ConfigurationAdmin cm;
	/**
	 * {@link QueryTask}s used to interface with the MCUBMU via a
	 * {@link io.openems.edge.bridge.mccomms.MCCommsBridge}
	 */
	private HashSet<QueryTask> queryTasks;
	/**
	 * {@link Config} for this component instance
	 */
	private Config config;
	/**
	 * Utility for mapping two input channels to one output channel (specifically
	 * for net current in this case)
	 */
	private NetCurrentChannelUpdater netCurrentChannelUpdater;

	/**
	 * {@inheritDoc}
	 */
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Signed value representing the current being used to charge the battery being
		 * managed by the MCUBMU
		 * <ul>
		 * <li>Type: Integer</li>
		 * <li>Unit: MilliAmperes</li>
		 * </ul>
		 */
		CHARGE_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE)),
		/**
		 * Signed value representing the current flowing out of the battery being
		 * managed by the MCUBMU
		 * <ul>
		 * <li>Type: Integer</li>
		 * <li>Unit: MilliAmperes</li>
		 * </ul>
		 */
		DISCHARGE_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Constructor
	 */
	public MCUBMU() {
		super(OpenemsComponent.ChannelId.values(), Battery.ChannelId.values(), ChannelId.values());
		this.queryTasks = new HashSet<>();
		this.netCurrentChannelUpdater = new NetCurrentChannelUpdater();
		channel(ChannelId.DISCHARGE_CURRENT)
				.onSetNextValue(value -> netCurrentChannelUpdater.dischargeCurrentChannelUpdated());
		channel(ChannelId.CHARGE_CURRENT)
				.onSetNextValue(value -> netCurrentChannelUpdater.chargeCurrentChannelUpdated());
	}

	/**
	 * {@link Reference}-annotated method to set the
	 * {@link io.openems.edge.bridge.mccomms.MCCommsBridge} for this device
	 * 
	 * @param bridge the {@link io.openems.edge.bridge.mccomms.MCCommsBridge} to use
	 *               to communicate with the device being controlled by this
	 *               component instance
	 */
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	public void setMCCommsBridge(IMCCommsBridge bridge) {
		super.setMCCommsBridge(bridge);
	}

	/**
	 * Activate method for this component class
	 * 
	 * @param context the OSGi {@link ComponentContext}
	 * @param config  {@link Config} for this component instance
	 */
	@Activate
	public void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.UBMUmcCommsAddress(), cm,
				config.mcCommsBridge_id());
		this.config = config;
		try {
			// noinspection unchecked
			queryTasks.add(constructQueryTask(56, 57,
					MCCommsElement.newInstanceFromChannel(7, 2, channel(Battery.ChannelId.SOC)).setScaleFactor(0.01),
					MCCommsElement.newInstanceFromChannel(9, 2, channel(Battery.ChannelId.SOH)).setScaleFactor(0.01),
					MCCommsElement.newInstanceFromChannel(11, 2, channel(Battery.ChannelId.VOLTAGE))
							.setScaleFactor(0.001),
					MCCommsElement.newInstanceFromChannel(13, 2, channel(ChannelId.CHARGE_CURRENT)).setScaleFactor(100),
					MCCommsElement.newInstanceFromChannel(15, 2, channel(ChannelId.DISCHARGE_CURRENT))
							.setScaleFactor(100),
					MCCommsBitSetElement.newInstanceFromChannels(18, 2, channel(Battery.ChannelId.READY_FOR_WORKING)))
							.queryRepeatedly(config.RTDrefreshMS(), TimeUnit.MILLISECONDS));
			queryTasks.add(constructQueryTask(58, 59,
					MCCommsElement.newInstanceFromChannel(7, 2, channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE))
							.setScaleFactor(0.001),
					MCCommsElement.newInstanceFromChannel(9, 2, channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE))
							.setScaleFactor(0.001),
					MCCommsElement.newInstanceFromChannel(11, 2, channel(Battery.ChannelId.CHARGE_MAX_CURRENT))
							.setScaleFactor(0.1),
					MCCommsElement.newInstanceFromChannel(13, 2, channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT))
							.setScaleFactor(0.1)).queryRepeatedly(config.RTDrefreshMS(), TimeUnit.MILLISECONDS));
			queryTasks.add(constructQueryTask(60, 61,
					MCCommsElement.newInstanceFromChannel(7, 2, channel(Battery.ChannelId.MAX_CELL_VOLTAGE)),
					MCCommsElement.newInstanceFromChannel(11, 2, channel(Battery.ChannelId.MIN_CELL_VOLTAGE)),
					MCCommsElement.newInstanceFromChannel(17, 1, channel(Battery.ChannelId.MAX_CELL_TEMPERATURE))
							.setUnsigned(false),
					MCCommsElement.newInstanceFromChannel(19, 1, channel(Battery.ChannelId.MIN_CELL_TEMPERATURE))
							.setUnsigned(false)).queryRepeatedly(config.statusRefreshMS(), TimeUnit.MILLISECONDS));
			queryTasks
					.add(constructQueryTask(64, 65,
							MCCommsElement.newInstanceFromChannel(19, 2, channel(Battery.ChannelId.CAPACITY))
									.setScaleFactor(100)).queryRepeatedly(config.statusRefreshMS(),
											TimeUnit.MILLISECONDS));
		} catch (OpenemsException e) {
			logError(logger, e.getMessage());
		}
	}

	/**
	 * Convenenience method to construct {@link QueryTask}s for this component
	 * 
	 * @param queryCommand           the command value to use when querying the
	 *                               MCUBMU
	 * @param responseCommand        the expected command value of the reply packet
	 * @param responsePacketElements the elements used to create a packet structure
	 *                               for the reply packet to map to
	 * @return a new {@link QueryTask}
	 * @throws OpenemsException if the {@link QueryTask could not be constructed}
	 */
	private QueryTask constructQueryTask(int queryCommand, int responseCommand,
			MCCommsElement... responsePacketElements) throws OpenemsException {
		// noinspection unchecked
		return QueryTask.newCommandOnlyQuery(getMCCommsBridge(), config.openemsMCCommsAddress(),
				config.UBMUmcCommsAddress(), queryCommand, config.queryTimeoutMS(), TimeUnit.MILLISECONDS,
				new ListenTask(config.UBMUmcCommsAddress(), config.openemsMCCommsAddress(), responseCommand,
						new MCCommsPacket(responsePacketElements)));
	}

	@Deactivate
	protected void deactivate() {
		for (QueryTask queryTask : queryTasks) {
			queryTask.cancel();
		}
		super.deactivate();
	}

	/**
	 * Private class that offsets the {@link ChannelId#CHARGE_CURRENT} and
	 * {@link ChannelId#DISCHARGE_CURRENT} channel values against each other to
	 * produce a net current value that can be mapped to the
	 * {@link Battery.ChannelId#CURRENT} channel
	 */
	private class NetCurrentChannelUpdater {
		/**
		 * Flag that signals if the {@link ChannelId#CHARGE_CURRENT} channel has been
		 * updated since the last time a value was mapped to the
		 * {@link Battery.ChannelId#CURRENT} channel
		 */
		private boolean chargeCurrentChannelUpdated;
		/**
		 * Flag that signals if the {@link ChannelId#DISCHARGE_CURRENT} channel has been
		 * updated since the last time a value was mapped to the
		 * {@link Battery.ChannelId#CURRENT} channel
		 */
		private boolean dischargeCurrentChannelUpdated;

		/**
		 * Constructor
		 */
		NetCurrentChannelUpdater() {
			chargeCurrentChannelUpdated = false;
			dischargeCurrentChannelUpdated = false;
		}

		/**
		 * Set the {@link NetCurrentChannelUpdater#dischargeCurrentChannelUpdated} flag
		 * to true and calls
		 * {@link NetCurrentChannelUpdater#tryUpdateNetCurrentChannelValue()}
		 */
		void dischargeCurrentChannelUpdated() {
			dischargeCurrentChannelUpdated = true;
			tryUpdateNetCurrentChannelValue();
		}

		/**
		 * Set the {@link NetCurrentChannelUpdater#chargeCurrentChannelUpdated} flag to
		 * true and calls
		 * {@link NetCurrentChannelUpdater#tryUpdateNetCurrentChannelValue()}
		 */
		void chargeCurrentChannelUpdated() {
			chargeCurrentChannelUpdated = true;
			tryUpdateNetCurrentChannelValue();
		}

		/**
		 * If both the {@link NetCurrentChannelUpdater#chargeCurrentChannelUpdated} and
		 * {@link NetCurrentChannelUpdater#dischargeCurrentChannelUpdated} flags are
		 * true, it calculates the net current and maps that value to the
		 * {@link Battery.ChannelId#CURRENT} channel
		 */
		private void tryUpdateNetCurrentChannelValue() {
			if (chargeCurrentChannelUpdated && dischargeCurrentChannelUpdated) {
				chargeCurrentChannelUpdated = false;
				dischargeCurrentChannelUpdated = false;
				CalculateIntegerSum sum = new CalculateIntegerSum();
				channel(ChannelId.DISCHARGE_CURRENT).nextProcessImage();
				sum.addValue(channel(ChannelId.DISCHARGE_CURRENT));
				channel(ChannelId.CHARGE_CURRENT).nextProcessImage();
				sum.addValue(channel(ChannelId.CHARGE_CURRENT));
				channel(Battery.ChannelId.CURRENT).setNextValue(sum.calculate());
			}
		}
	}
}
