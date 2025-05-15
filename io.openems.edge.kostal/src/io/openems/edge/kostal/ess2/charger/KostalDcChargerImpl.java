package io.openems.edge.kostal.ess2.charger;

import static io.openems.edge.bridge.modbus.api.element.WordOrder.LSWMSW;

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
import io.openems.edge.kostal.common.AbstractSunSpecDcCharger;
import io.openems.edge.kostal.ess2.KostalManagedEss;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(
		//
		name = "Kostal.Plenticore.DcCharger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //

)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})

public class KostalDcChargerImpl extends AbstractSunSpecDcCharger
		implements
			KostalDcCharger,
			EssDcCharger,
			ModbusComponent,
			OpenemsComponent,
			EventHandler,
			TimedataProvider,
			ModbusSlave {

	private static final int READ_FROM_MODBUS_BLOCK = 1;

	@Reference
	protected ConfigurationAdmin cm;

	private final CalculateEnergyFromPower calculateActualEnergy = new CalculateEnergyFromPower(
			this, EssDcCharger.ChannelId.ACTUAL_ENERGY);

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	public KostalDcChargerImpl() throws OpenemsException {
		super(//
				ACTIVE_MODELS, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				KostalDcCharger.ChannelId.values() //
		);

		this.addStaticModbusTasks(this.getModbusProtocol());
	}

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private KostalManagedEss ess;

	@Activate
	void activate(ComponentContext context, Config config)
			throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(),
				config.enabled(), this.ess.getUnitId(), this.cm, "Modbus",
				this.ess.getModbusBridgeId(), READ_FROM_MODBUS_BLOCK)) {
			return;
		}

		try {
			// update filter for 'Ess'
			if (OpenemsComponent.updateReferenceFilter(this.cm,
					this.servicePid(), "ess", config.ess_id())) {
				return;
			}

			this.ess.addCharger(this);
		} catch (Exception e) {
			// TODO proper error handling
			e.printStackTrace();
		}
	}

	private static final Map<SunSpecModel, Priority> ACTIVE_MODELS = ImmutableMap
			.<SunSpecModel, Priority>builder()
			.put(DefaultSunSpecModel.S_1, Priority.LOW) //
			// .put(DefaultSunSpecModel.S_103, Priority.LOW) //
			.put(DefaultSunSpecModel.S_113, Priority.LOW) //
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
				DefaultSunSpecModel.S113.DCV, DefaultSunSpecModel.S103.DCV);

		this.mapFirstPointToChannel(//
				EssDcCharger.ChannelId.CURRENT, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S113.DCA, DefaultSunSpecModel.S103.DCA);

		// TODO required? as register 1066 is read
		this.mapFirstPointToChannel(//
				KostalDcCharger.ChannelId.PRODUCTION_POWER, //
				ElementToChannelConverter.DIRECT_1_TO_1, //
				DefaultSunSpecModel.S113.DCW, DefaultSunSpecModel.S103.DCW);

		// this.mapFirstPointToChannel(//
		// EssDcCharger.ChannelId.ACTUAL_ENERGY, //
		// ElementToChannelConverter.DIRECT_1_TO_1, //
		// DefaultSunSpecModel.S113.WH, DefaultSunSpecModel.S103.WH);

	}

	/**
	 * Adds static modbus tasks.
	 * 
	 * @param protocol
	 *            the {@link ModbusProtocol}
	 * @throws OpenemsException
	 *             on error
	 */
	private void addStaticModbusTasks(ModbusProtocol protocol)
			throws OpenemsException {
		protocol.addTask(//
				new FC3ReadRegistersTask(1066, Priority.HIGH, //
						m(EssDcCharger.ChannelId.ACTUAL_POWER,
								new FloatDoublewordElement(1066)
										.wordOrder(LSWMSW))));

	}

	@Override
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
			case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE :
				this.calculateActualEnergy.update(this.getActualPower().get());
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
				ModbusSlaveNatureTable
						.of(KostalDcCharger.class, accessMode, 100) //
						.build());
	}
}
