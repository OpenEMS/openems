package io.openems.edge.goodwe.charger.twostring;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.goodwe.charger.AbstractGoodWeEtCharger;
import io.openems.edge.goodwe.charger.GoodWeCharger;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.Charger.Two-String", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class GoodWeChargerTwoStringImpl extends AbstractGoodWeEtCharger implements GoodWeChargerTwoString, EssDcCharger,
		GoodWeCharger, ModbusComponent, OpenemsComponent, EventHandler, TimedataProvider, ModbusSlave {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private GoodWe essOrBatteryInverter;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	private PvPort pvPort;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public GoodWeChargerTwoStringImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				GoodWeCharger.ChannelId.values(), //
				GoodWeChargerTwoString.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.pvPort = config.pvPort();
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "essOrBatteryInverter",
				config.essOrBatteryInverter_id())) {
			return;
		}

		this.essOrBatteryInverter.addCharger(this);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		this.essOrBatteryInverter.removeCharger(this);
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() throws OpenemsException {
		final var mpptPowerAddress = this.pvPort.mpptPowerAddress;
		final var mpptCurrentAddress = this.pvPort.mpptCurrentAddress;
		final var pvStartAddress = this.pvPort.pvStartAddress;
		final var modbusProtocol = new ModbusProtocol(this, //

				// Voltage & current of single MPPT string
				new FC3ReadRegistersTask(pvStartAddress, Priority.HIGH, //
						m(EssDcCharger.ChannelId.VOLTAGE, new UnsignedWordElement(pvStartAddress),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(EssDcCharger.ChannelId.CURRENT, new UnsignedWordElement(pvStartAddress + 1),
								ElementToChannelConverter.SCALE_FACTOR_2)),

				// Total MPPT power
				new FC3ReadRegistersTask(mpptPowerAddress, Priority.HIGH, //
						m(GoodWeChargerTwoString.ChannelId.TOTAL_MPPT_POWER,
								new UnsignedWordElement(mpptPowerAddress))),

				// Total MPPT current
				new FC3ReadRegistersTask(mpptCurrentAddress, Priority.HIGH, //
						m(GoodWeChargerTwoString.ChannelId.TOTAL_MPPT_CURRENT,
								new UnsignedWordElement(mpptCurrentAddress), //
								ElementToChannelConverter.SCALE_FACTOR_2))

		);

		// Calculate power of single MPPT string
		this.addCalculateChannelListeners();

		return modbusProtocol;
	}

	/**
	 * Calculates required Channels from other existing Channels.
	 */
	private void addCalculateChannelListeners() {

		// Get actual Channels
		var totalMpptPowerChannel = this.getTotalMpptPowerChannel();
		var totalMpptCurrentChannel = this.getTotalMpptCurrentChannel();
		var stringCurrentChannel = this.getCurrentChannel();

		// Power Value from the total MPPT power and current values
		final Consumer<Value<Integer>> calculatePower = ignore -> {
			// TODO: Calculate based on the related string
			this._setActualPower(//
					calculateByRuleOfThree(//
							totalMpptPowerChannel.getNextValue().asOptional(), //
							totalMpptCurrentChannel.getNextValue().asOptional(), //
							stringCurrentChannel.getNextValue().asOptional()) //
							// If at least one value was present, the result should not be null.
							.orElse(0) //
			);
		};

		// Add Listeners
		totalMpptPowerChannel.onSetNextValue(calculatePower);
		stringCurrentChannel.onSetNextValue(calculatePower);
		totalMpptCurrentChannel.onSetNextValue(calculatePower);
	}

	/**
	 * Calculate a value by rule of three.
	 * 
	 * <p>
	 * Solves proportions and calculate the unknown value.
	 * 
	 * <p>
	 * Assure that the unit of the divisor and relatedValue are the same.
	 * 
	 * @param total   total optional of the required unit
	 * @param divisor divisor of the known unit
	 * @param related related optional with the known unit
	 * @return the calculated result. Return null for empty parameters or zero
	 *         divisor
	 */
	public static Optional<Integer> calculateByRuleOfThree(Optional<Integer> total, Optional<Integer> divisor,
			Optional<Integer> related) {

		var result = new AtomicReference<Integer>(null);
		total.ifPresent(totalValue -> {
			divisor.ifPresent(divisorValue -> {
				related.ifPresent(relatedValue -> {
					if (divisorValue == 0) {
						return;
					}

					/*
					 * As the total power of the charger is sometimes less than the power of an
					 * individual string, the minimum is taken.
					 * 
					 * TODO: Remove it if it has been fixed by GoodWe
					 */
					result.set(Math.round((totalValue * relatedValue) / divisorValue.floatValue()));
				});
			});
		});
		return Optional.ofNullable(result.get());
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				EssDcCharger.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(GoodWeChargerTwoString.class, accessMode, 100) //
						.build());
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	protected GoodWe getEssOrBatteryInverter() {
		return this.essOrBatteryInverter;
	}

	@Override
	protected int getStartAddress() {
		// Not used because the defineModbusProtocol is not generic by the start address
		return 0;
	}
}
