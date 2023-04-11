package io.openems.edge.solaredge.charger;

import java.util.Map;

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

import com.google.common.collect.ImmutableMap;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;

import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.WordOrder;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.sunspec.DefaultSunSpecModel;
import io.openems.edge.bridge.modbus.sunspec.SunSpecModel;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;

import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.ess.dccharger.api.EssDcCharger;

import io.openems.edge.ess.sunspec.AbstractSunSpecDcCharger;
import io.openems.edge.solaredge.hybrid.ess.SolarEdgeHybridEss;

import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "io.openems.edge.solaredge.charger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //

)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})

public class SolaredgeDcChargerImpl extends AbstractSunSpecDcCharger implements SolaredgeDcCharger, EssDcCharger,
		ModbusComponent, OpenemsComponent, EventHandler, TimedataProvider, ModbusSlave {

	private static final int READ_FROM_MODBUS_BLOCK = 1;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	public SolaredgeDcChargerImpl() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				SolaredgeDcCharger.ChannelId.values() //
		);

		addStaticModbusTasks(this.getModbusProtocol());
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SolarEdgeHybridEss ess;

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), this.ess.getUnitId(), this.cm,
				"Modbus", this.ess.getModbusBridgeId(), READ_FROM_MODBUS_BLOCK)) {
			return;
		}

		// update filter for 'Ess'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "Ess", config.ess_id())) {
			return;
		}

		this.ess.addCharger(this);
	}

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			.put(DefaultSunSpecModel.S_103, Priority.LOW) //
			.put(DefaultSunSpecModel.S_120, Priority.LOW) //
			// .put(DefaultSunSpecModel.S_203, Priority.LOW) //

			// .put(DefaultSunSpecModel.S_802, Priority.LOW) //

			/*
			 * .put(DefaultSunSpecModel.S_203, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_101, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_102, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_103, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_111, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_112, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_113, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_120, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_121, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_122, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_123, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_124, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_125, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_127, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_128, Priority.LOW) //
			 * .put(DefaultSunSpecModel.S_145, Priority.LOW) //
			 */
			.build();

	@Override
	protected void onSunSpecInitializationCompleted() {
		// TODO Add mappings for registers from S1 and S103

		// Example:
		// this.mapFirstPointToChannel(//
		// SymmetricEss.ChannelId.ACTIVE_POWER, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S103.W);

		// this.mapFirstPointToChannel(//
		// SymmetricEss.ChannelId.CONSUMPTION_POWER, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S103.W);

		// DefaultSunSpecModel.S103.W);

		this.mapFirstPointToChannel(//
				EssDcCharger.ChannelId.VOLTAGE, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S103.DCV);

		this.mapFirstPointToChannel(//
				EssDcCharger.ChannelId.CURRENT, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S103.DCA);

		this.mapFirstPointToChannel(//
				SolaredgeDcCharger.ChannelId.PRODUCTION_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S103.DCW);

		this.mapFirstPointToChannel(//
				EssDcCharger.ChannelId.ACTUAL_ENERGY, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S103.WH);

	}

	/**
	 * Adds static modbus tasks.
	 * 
	 * @param protocol the {@link ModbusProtocol}
	 * @throws OpenemsException on error
	 */
	private void addStaticModbusTasks(ModbusProtocol protocol) throws OpenemsException {
		protocol.addTask(//
				new FC3ReadRegistersTask(0xE174, Priority.HIGH, //
						m(SolaredgeDcCharger.ChannelId.DC_DISCHARGE_POWER, // Instantaneous Power from Solaregde - no
																			// scaling
								new FloatDoublewordElement(0xE174).wordOrder(WordOrder.LSWMSW)) //
				));

		protocol.addTask(//
				new FC3ReadRegistersTask(0x9CA4, Priority.HIGH, //

						m(SolarEdgeHybridEss.ChannelId.POWER_DC, //
								new SignedWordElement(0x9CA4)),
						m(SolarEdgeHybridEss.ChannelId.POWER_DC_SCALE, //
								new SignedWordElement(0x9CA5))));

	}

	public void _calculateAndSetActualPower() {
		// Aktuelle Erzeugung durch den Hybrid-WR ist der aktuelle Verbrauch +
		// Batterie-Ladung/Entladung *-1
		// Actual power from inverter comes from house consumption + battery inverter
		// power (*-1)
		try {
			int dcPower = this.getDcPower().get(); // Leistung Inverter
			int dcPowerScale = this.getDcPowerScale().get(); // Leistung Inverter
			double dcPowerValue = dcPower * Math.pow(10, dcPowerScale);

			int dcDischargePower = this.getDcDischargePower().get();
			int pvDcProduction = (int) dcPowerValue + dcDischargePower;

			if (pvDcProduction < 0)
				pvDcProduction = 0; // Negative Values are not allowed for PV production

			this._setActualPower(pvDcProduction);
		} catch (Exception e) {
			return;
		}
	}

	@Deactivate
	protected void deactivate() {
		this.ess.removeCharger(this);
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			_calculateAndSetActualPower();
			break;
		}
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				EssDcCharger.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(SolaredgeDcCharger.class, accessMode, 100) //
						.build());
	}

}
