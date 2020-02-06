package io.openems.edge.battery.microcare.ubmu;

import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import io.openems.common.channel.AccessMode;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.DoubleReadChannel;
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
import io.openems.edge.bridge.mccomms.packet.MCCommsScalerDuplexElement;
import io.openems.edge.bridge.mccomms.packet.MCCommsScalerElement;
import io.openems.edge.bridge.mccomms.task.ListenTask;
import io.openems.edge.bridge.mccomms.task.QueryTask;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.value.Value;
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
		 * <li>Type: Double</li>
		 * <li>Unit: Amperes</li>
		 * </ul>
		 */
		CHARGE_CURRENT(Doc.of(OpenemsType.DOUBLE).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		/**
		 * Signed value representing the current flowing out of the battery being
		 * managed by the MCUBMU
		 * <ul>
		 * <li>Type: Double</li>
		 * <li>Unit: Amperes</li>
		 * </ul>
		 */
		DISCHARGE_CURRENT(Doc.of(OpenemsType.DOUBLE).unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
		/**
		 * Value for the scaling factor for percentage values
		 * <ul>
		 * <li>Type: Double</li>
		 * <li>Unit: None</li>
		 * </ul>
		 */
		PERCENTAGE_SCALER(Doc.of(OpenemsType.DOUBLE).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		/**
		 * Value for the scaling factor for voltage limit values
		 * <ul>
		 * <li>Type: Double</li>
		 * <li>Unit: None</li>
		 * </ul>
		 */
		VOLTAGE_LIMIT_SCALER(Doc.of(OpenemsType.DOUBLE).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		/**
		 * Value for the scaling factor for battery voltage values
		 * <ul>
		 * <li>Type: Double</li>
		 * <li>Unit: None</li>
		 * </ul>
		 */
		BATTERY_VOLTAGE_SCALER(Doc.of(OpenemsType.DOUBLE).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		/**
		 * Value for the scaling factor for cell voltage values
		 * <ul>
		 * <li>Type: Double</li>
		 * <li>Unit: None</li>
		 * </ul>
		 */
		CELL_VOLTAGE_SCALER(Doc.of(OpenemsType.DOUBLE).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		/**
		 * Value for the scaling factor for battery current values
		 * <ul>
		 * <li>Type: Double</li>
		 * <li>Unit: None</li>
		 * </ul>
		 */
		BATTERY_CURRENT_SCALER(Doc.of(OpenemsType.DOUBLE).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		/**
		 * Value for the scaling factor for battery amp hour values
		 * <ul>
		 * <li>Type: Double</li>
		 * <li>Unit: None</li>
		 * </ul>
		 */
		BATTERY_AMP_HOURS_SCALER(Doc.of(OpenemsType.DOUBLE).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		/**
		 * Value for the scaling factor for battery watt hour values
		 * <ul>
		 * <li>Type: Double</li>
		 * <li>Unit: None</li>
		 * </ul>
		 */
		BATTERY_WATT_HOURS_SCALER(Doc.of(OpenemsType.DOUBLE).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		/**
		 * Value for the scaling factor for cell celsius temperature values
		 * <ul>
		 * <li>Type: Double</li>
		 * <li>Unit: None</li>
		 * </ul>
		 */
		CELL_TEMPERATURE_SCALER(Doc.of(OpenemsType.DOUBLE).unit(Unit.NONE).accessMode(AccessMode.READ_ONLY)),
		//TODO JavaDoc comments
	    GENERAL_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
	    BATTERY_HIGH_VOLTAGE_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
	    BATTERY_LOW_VOLTAGE_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
	    BATTERY_HIGH_TEMPERATURE_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
	    BATTERY_LOW_TEMPERATURE_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
	    BATTERY_HIGH_TEMPERATURE_CHARGE_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
	    BATTERY_LOW_TEMPERATURE_CHARGE_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
	    BATTERY_HIGH_DISCHARGE_CURRENT_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
	    BATTERY_HIGH_CHARGE_CURRENT_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
	    CONTACTOR_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
	    SHORT_CIRCUIT_OR_OPEN_CELL_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
		BMS_INTERNAL_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
	    CELL_IMBALANCE_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
	    BATTERY_COMMUNICATION_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
	    BMU_CRITICAL_VALUE_READ_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
	    BAD_CONFIGURATION_ERROR(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY));
	    
	    
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
		
		//Calculate net current on value updates
		channel(ChannelId.DISCHARGE_CURRENT)
				.onSetNextValue(value -> netCurrentChannelUpdater.dischargeCurrentChannelUpdated());
		channel(ChannelId.CHARGE_CURRENT)
				.onSetNextValue(value -> netCurrentChannelUpdater.chargeCurrentChannelUpdated());
		
		//reset READY_FOR_WORKING to true when image is processed
		channel(io.openems.edge.battery.api.Battery.ChannelId.READY_FOR_WORKING).onUpdate(value -> {
			channel(io.openems.edge.battery.api.Battery.ChannelId.READY_FOR_WORKING).setNextValue(true);
		});
		
		//callbacks to set READY_FOR_WORKING state on error channel value updates
		((BooleanReadChannel) channel(ChannelId.GENERAL_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.BATTERY_HIGH_VOLTAGE_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.BAD_CONFIGURATION_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.BATTERY_COMMUNICATION_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.BATTERY_HIGH_CHARGE_CURRENT_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.BATTERY_HIGH_DISCHARGE_CURRENT_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.BATTERY_HIGH_TEMPERATURE_CHARGE_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.BATTERY_HIGH_TEMPERATURE_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.BATTERY_HIGH_VOLTAGE_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.BATTERY_LOW_TEMPERATURE_CHARGE_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.BATTERY_LOW_TEMPERATURE_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.BATTERY_LOW_VOLTAGE_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.BMS_INTERNAL_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.BMU_CRITICAL_VALUE_READ_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.CELL_IMBALANCE_ERROR))
				.onSetNextValue(this::setReadyState);
		((BooleanReadChannel) channel(ChannelId.SHORT_CIRCUIT_OR_OPEN_CELL_ERROR))
				.onSetNextValue(this::setReadyState);

		//percentage scaler value
		channel(ChannelId.PERCENTAGE_SCALER).setNextValue(0.01); //this scaler is static regardless of battery type
		channel(ChannelId.PERCENTAGE_SCALER).nextProcessImage();
	}
	
	/**
	 * Callback method to set the ready state to false if an error is present
	 * @param errorIsPresent true if an error is present
	 */
	private void setReadyState(Value<Boolean> errorIsPresent) {
		if (errorIsPresent.get()) {
			channel(io.openems.edge.battery.api.Battery.ChannelId.READY_FOR_WORKING).setNextValue(false);
		}
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
			//RTD1
			queryTasks.add
				(
					constructQueryTask(56, 57,
						MCCommsScalerDuplexElement.newInstanceFromChannels(17, channel(ChannelId.BATTERY_VOLTAGE_SCALER), channel(ChannelId.BATTERY_CURRENT_SCALER)),
						MCCommsElement.newInstanceFromChannel(7, 2, channel(ChannelId.PERCENTAGE_SCALER), channel(Battery.ChannelId.SOC)),
						MCCommsElement.newInstanceFromChannel(9, 2, channel(ChannelId.PERCENTAGE_SCALER), channel(Battery.ChannelId.SOH)),
						MCCommsElement.newInstanceFromChannel(11, 2, channel(ChannelId.BATTERY_VOLTAGE_SCALER), channel(Battery.ChannelId.VOLTAGE)),
						MCCommsElement.newInstanceFromChannel(13, 2, channel(ChannelId.BATTERY_CURRENT_SCALER), channel(ChannelId.CHARGE_CURRENT)),
						MCCommsElement.newInstanceFromChannel(15, 2, channel(ChannelId.BATTERY_CURRENT_SCALER), channel(ChannelId.DISCHARGE_CURRENT)),
						MCCommsBitSetElement.newInstanceFromChannels(18, 2, 
							channel(ChannelId.GENERAL_ERROR),
							channel(ChannelId.BATTERY_HIGH_VOLTAGE_ERROR),
							channel(ChannelId.BATTERY_LOW_VOLTAGE_ERROR),
							channel(ChannelId.BATTERY_HIGH_TEMPERATURE_ERROR),
							channel(ChannelId.BATTERY_LOW_TEMPERATURE_ERROR),
							channel(ChannelId.BATTERY_HIGH_TEMPERATURE_CHARGE_ERROR),
							channel(ChannelId.BATTERY_LOW_TEMPERATURE_CHARGE_ERROR),
							channel(ChannelId.BATTERY_HIGH_DISCHARGE_CURRENT_ERROR),
							channel(ChannelId.BATTERY_HIGH_CHARGE_CURRENT_ERROR),
							channel(ChannelId.CONTACTOR_ERROR),
							channel(ChannelId.SHORT_CIRCUIT_OR_OPEN_CELL_ERROR),
							channel(ChannelId.BMS_INTERNAL_ERROR),
							channel(ChannelId.CELL_IMBALANCE_ERROR),
							channel(ChannelId.BATTERY_COMMUNICATION_ERROR),
							channel(ChannelId.BMU_CRITICAL_VALUE_READ_ERROR),
							channel(ChannelId.BAD_CONFIGURATION_ERROR)							
						)
					).queryRepeatedly(config.RTDrefreshMS(), TimeUnit.MILLISECONDS)
				);
			//RTD2
			queryTasks.add
				(
					constructQueryTask(58, 59,
						MCCommsScalerDuplexElement.newInstanceFromChannels(17, channel(ChannelId.VOLTAGE_LIMIT_SCALER), channel(ChannelId.BATTERY_CURRENT_SCALER)),
						MCCommsElement.newInstanceFromChannel(7, 2, channel(ChannelId.VOLTAGE_LIMIT_SCALER), channel(Battery.ChannelId.CHARGE_MAX_VOLTAGE)),
						MCCommsElement.newInstanceFromChannel(9, 2, channel(ChannelId.VOLTAGE_LIMIT_SCALER), channel(Battery.ChannelId.DISCHARGE_MIN_VOLTAGE)),
						MCCommsElement.newInstanceFromChannel(11, 2, channel(ChannelId.BATTERY_CURRENT_SCALER), channel(Battery.ChannelId.CHARGE_MAX_CURRENT))
							.setUnsigned(false),
						MCCommsElement.newInstanceFromChannel(13, 2, channel(ChannelId.BATTERY_CURRENT_SCALER), channel(Battery.ChannelId.DISCHARGE_MAX_CURRENT))
							.setUnsigned(false)
					).queryRepeatedly(config.RTDrefreshMS(), TimeUnit.MILLISECONDS)
				);
			//Cell Data
			queryTasks.add
				(
					constructQueryTask(60, 61,
						MCCommsScalerElement.newInstanceFromChannel(20, channel(ChannelId.CELL_VOLTAGE_SCALER)),
						MCCommsElement.newInstanceFromChannel(7, 2, channel(ChannelId.CELL_VOLTAGE_SCALER), channel(Battery.ChannelId.MAX_CELL_VOLTAGE)),
						MCCommsElement.newInstanceFromChannel(11, 2, channel(ChannelId.CELL_VOLTAGE_SCALER), channel(Battery.ChannelId.MIN_CELL_VOLTAGE)),
						MCCommsElement.newInstanceFromChannel(17, 1, null, channel(Battery.ChannelId.MAX_CELL_TEMPERATURE))
							.setUnsigned(false),
						MCCommsElement.newInstanceFromChannel(19, 1, null, channel(Battery.ChannelId.MIN_CELL_TEMPERATURE))
							.setUnsigned(false)
					).queryRepeatedly(config.statusRefreshMS(), TimeUnit.MILLISECONDS)
				);
			//Capacity
			queryTasks.add
				(
					constructQueryTask(64, 65,
							MCCommsScalerDuplexElement.newInstanceFromChannels(17, channel(ChannelId.BATTERY_WATT_HOURS_SCALER), channel(ChannelId.BATTERY_AMP_HOURS_SCALER)),
							MCCommsElement.newInstanceFromChannel(19, 2,channel(ChannelId.BATTERY_WATT_HOURS_SCALER), channel(Battery.ChannelId.CAPACITY))
					).queryRepeatedly(config.statusRefreshMS(),TimeUnit.MILLISECONDS)
				);
		} catch (OpenemsException e) {
			logError(logger, e.getMessage());
		}
	}

	/**
	 * Convenience method to construct {@link QueryTask}s for this component
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
		return QueryTask.newCommandOnlyQuery(getMCCommsBridge(), config.openemsMCCommsAddress(),
				config.UBMUmcCommsAddress(), queryCommand, config.queryTimeoutMS(), TimeUnit.MILLISECONDS,
				new ListenTask(config.UBMUmcCommsAddress(), config.openemsMCCommsAddress(), responseCommand,
						new MCCommsPacket(responsePacketElements)));
	}

	/**
	 * {@inheritDoc}
	 */
	@Deactivate
	protected void deactivate() {
		for (QueryTask queryTask : queryTasks) {
			queryTask.cancel();
		}
		super.deactivate();
	}

	/**
	 * Copied from {@link io.openems.edge.battery.soltaro.single.versiona.SingleRack#debugLog()}
	 * {@inheritDoc}
	 */
	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().value() //
				+ "|Discharge:" + this.getDischargeMinVoltage().value() + ";" + this.getDischargeMaxCurrent().value() //
				+ "|Charge:" + this.getChargeMaxVoltage().value() + ";" + this.getChargeMaxCurrent().value();
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
				double dischargeCurrent = ((DoubleReadChannel) channel(ChannelId.DISCHARGE_CURRENT)).getNextValue().get();
				double chargeCurrent = ((DoubleReadChannel) channel(ChannelId.CHARGE_CURRENT)).getNextValue().get();
				double netCurrent;
				if (dischargeCurrent == chargeCurrent) {
					netCurrent = dischargeCurrent;
				} else {
					netCurrent = dischargeCurrent - chargeCurrent;
				}
				channel(Battery.ChannelId.CURRENT).setNextValue(netCurrent);
			}
		}
	}
}
