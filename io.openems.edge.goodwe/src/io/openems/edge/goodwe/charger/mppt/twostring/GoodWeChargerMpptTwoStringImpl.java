package io.openems.edge.goodwe.charger.mppt.twostring;

import java.util.Optional;
import java.util.function.Consumer;

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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.goodwe.charger.GoodWeCharger;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.Charger.Mppt.Two-String", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class GoodWeChargerMpptTwoStringImpl extends AbstractOpenemsComponent
		implements EssDcCharger, GoodWeCharger, OpenemsComponent, EventHandler, TimedataProvider, ModbusSlave {

	private final CalculateEnergyFromPower calculateActualEnergy = new CalculateEnergyFromPower(this,
			EssDcCharger.ChannelId.ACTUAL_ENERGY);

	private GoodWeListener currentListener;
	private GoodWeListener powerListener;
	private GoodWeListener voltageListener;
	private Config config;
	private boolean timedataQueryIsRunning = false;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private GoodWe essOrBatteryInverter;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	public GoodWeChargerMpptTwoStringImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				GoodWeCharger.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		this.currentListener = new GoodWeListener(this, this.essOrBatteryInverter,
				config.mpptPort().mpptCurrentChannelId, EssDcCharger.ChannelId.CURRENT);
		this.powerListener = new GoodWeListener(this, this.essOrBatteryInverter, config.mpptPort().mpptPowerChannelId,
				EssDcCharger.ChannelId.ACTUAL_POWER);
		this.voltageListener = new GoodWeListener(this, this.essOrBatteryInverter,
				config.mpptPort().mpptVoltageChannelId, EssDcCharger.ChannelId.VOLTAGE);

		this.essOrBatteryInverter.addCharger(this);

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "essOrBatteryInverter",
				config.essOrBatteryInverter_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.essOrBatteryInverter.removeCharger(this);
		this.currentListener.deactivate();
		this.powerListener.deactivate();
		this.voltageListener.deactivate();
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.calculateEnergy();
			break;
		}
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				EssDcCharger.getModbusSlaveNatureTable(accessMode));
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	/**
	 * Calculate the Energy values from ActivePower.
	 */
	private void calculateEnergy() {

		if (!this.getActualEnergy().isDefined()) {
			this.initializeCumulatedEnergyFromTimedata();
			return;
		}

		var actualPower = this.getActualPower().get();
		if (actualPower == null) {
			// Not available
			this.calculateActualEnergy.update(null);
		} else if (actualPower > 0) {
			this.calculateActualEnergy.update(actualPower);
		} else {
			this.calculateActualEnergy.update(0);
		}
	}

	/**
	 * Initialize cumulated energy value from from Timedata service.
	 */
	private void initializeCumulatedEnergyFromTimedata() {

		final var actualEnergyChannel = EssDcCharger.ChannelId.ACTUAL_ENERGY;
		var timedata = this.getTimedata();

		if (timedata == null || this.timedataQueryIsRunning) {
			return;
		}

		this.timedataQueryIsRunning = true;
		timedata.getLatestValue(new ChannelAddress(this.id(), actualEnergyChannel.id())).thenAccept(currentEnergy -> {

			if (currentEnergy.isEmpty()) {

				final String[] stringIds = switch (this.config.mpptPort()) {
				case MPPT_1 -> new String[] { "charger0", "charger1" };
				case MPPT_2 -> new String[] { "charger2", "charger3" };
				case MPPT_3 -> new String[] { "charger4", "charger5" };
				};

				/*
				 * Calculate total base energy from separate string energy values
				 */
				timedata.getLatestValueOfNotExistingChannel(new ChannelAddress(stringIds[0], actualEnergyChannel.id()),
						actualEnergyChannel.doc().getUnit())
						.thenCombine(timedata.getLatestValueOfNotExistingChannel(
								new ChannelAddress(stringIds[1], actualEnergyChannel.id()),
								actualEnergyChannel.doc().getUnit()), (energyString1, energyString2) -> {
									return caculateEnergyFromTwoStrings(energyString1, energyString2);
								})
						.thenAccept(combinedEnergy -> {
							this.channel(actualEnergyChannel).setNextValue(combinedEnergy);
							this.calculateActualEnergy.setBaseEnergyManually(combinedEnergy);
						});
			} else {
				this.channel(actualEnergyChannel)
						.setNextValue(TypeUtils.getAsType(OpenemsType.LONG, currentEnergy.get()));
			}
		});
	}

	/**
	 * Calculate energy from two given strings.
	 * 
	 * @param energyString1Opt energy of string 1
	 * @param energyString2Opt energy of string 2
	 * @return total energy
	 */
	public static Long caculateEnergyFromTwoStrings(Optional<Object> energyString1Opt,
			Optional<Object> energyString2Opt) {
		long energyString1;
		long energyString2;

		try {
			energyString1 = TypeUtils.getAsType(OpenemsType.LONG, energyString1Opt.get());
		} catch (Exception e) {
			energyString1 = 0L;
		}
		try {
			energyString2 = TypeUtils.getAsType(OpenemsType.LONG, energyString2Opt.get());
		} catch (Exception e) {
			energyString2 = 0L;
		}
		return TypeUtils.sum(energyString1, energyString2);
	}

	@Override
	public final String debugLog() {
		return "L:" + this.getActualPower().asString();
	}

	private static class GoodWeListener implements Consumer<Value<Integer>> {

		private final IntegerReadChannel goodWeChannel;
		private final IntegerReadChannel mirrorChannel;

		public GoodWeListener(GoodWeChargerMpptTwoStringImpl parent, GoodWe essOrBatteryInverter,
				GoodWe.ChannelId goodWeChannel, io.openems.edge.common.channel.ChannelId mirrorChannel) {
			this.goodWeChannel = essOrBatteryInverter.channel(goodWeChannel);
			this.mirrorChannel = parent.channel(mirrorChannel);
			this.goodWeChannel.onSetNextValue(this);
		}

		public void deactivate() {
			this.goodWeChannel.removeOnSetNextValueCallback(this);
		}

		@Override
		public void accept(Value<Integer> t) {
			this.mirrorChannel.setNextValue(t);
		}
	}
}
