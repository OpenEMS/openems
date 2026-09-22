package io.openems.edge.ess.stabl;

import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_3;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Modified;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.bridge.http.api.BridgeHttp;
import io.openems.common.bridge.http.api.BridgeHttpFactory;
import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.ModbusElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC4ReadInputRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.common.channel.ChannelId.ChannelIdImpl;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.internal.OpenemsTypeDoc;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.Phase.SinglePhase;
import io.openems.edge.ess.api.AsymmetricEss;
import io.openems.edge.ess.api.ManagedSinglePhaseEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SinglePhaseEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.stabl.statemachine.StateMachine;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "ESS.Stabl", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class EssStablImpl extends AbstractOpenemsModbusComponent
		implements ManagedSinglePhaseEss, SinglePhaseEss, AsymmetricEss, ManagedSymmetricEss, SymmetricEss, EssStabl,
		EventHandler, ModbusComponent, OpenemsComponent, ModbusSlave {

	private static final Logger log = LoggerFactory.getLogger(EssStablImpl.class);

	private SinglePhase singlePhase = null;
	private Config config = null;
	private final StateMachine stateMachine = new StateMachine(this);
	private final HttpPollEssSolver httpPollEssSolver = new HttpPollEssSolver();
	private final PowerReadingsUpdater powerReadingsUpdater = new PowerReadingsUpdater(this);
	private int string1InitializedCount = 0;
	private int string2InitializedCount = 0;
	private int string3InitializedCount = 0;

	@Reference
	private BridgeHttpFactory httpBridgeFactory;
	private BridgeHttp httpBridge;

	@Reference
	private Power power;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public EssStablImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				SymmetricEss.ChannelId.values(), //
				ManagedSymmetricEss.ChannelId.values(), //
				AsymmetricEss.ChannelId.values(), //
				SinglePhaseEss.ChannelId.values(), //
				ManagedSinglePhaseEss.ChannelId.values(), //
				EssStabl.ChannelId.values() //
		);
		this._setMaxApparentPower(EssStableConstants.MAX_APPARENT_POWER);
		this.powerReadingsUpdater.registerListeners();
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}

		this.httpBridge = this.httpBridgeFactory.get();
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.httpBridgeFactory.unget(this.httpBridge);
		this.httpBridge = null;
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(1016, Priority.LOW, //
						m(EssStabl.ChannelId.COM_TIME_OUT_EMS, new UnsignedWordElement(1016))),
				new FC3ReadRegistersTask(1018, Priority.LOW, //
						m(EssStabl.ChannelId.STANDBY_MODE, new UnsignedWordElement(1018))),
				new FC3ReadRegistersTask(4000, Priority.HIGH,
						m(EssStabl.ChannelId.ACTIVATE_POWER_STAGE, new UnsignedWordElement(4000)),
						new DummyRegisterElement(4001),
						m(EssStabl.ChannelId.RESET_ALARM_FLAG, new UnsignedWordElement(4002)),
						new DummyRegisterElement(4003, 4008),
						m(EssStabl.ChannelId.GRID_TYPE, new UnsignedWordElement(4009)),
						m(EssStabl.ChannelId.GRID_CODE, new UnsignedWordElement(4010))),
				new FC3ReadRegistersTask(4195, Priority.HIGH,
						m(EssStabl.ChannelId.SYSTEM_SIGNED_POWER_SET_POINT_AC, new SignedWordElement(4195)),
						m(EssStabl.ChannelId.COS_PHI_SET_POINT_AC, new SignedWordElement(4196), SCALE_FACTOR_MINUS_3)),
				new FC3ReadRegistersTask(4222, Priority.LOW,
						m(EssStabl.ChannelId.ISLAND_FREQUENCY_OFFSET, new UnsignedWordElement(4222),
								SCALE_FACTOR_MINUS_3)),
				new FC3ReadRegistersTask(6000, Priority.HIGH,
						m(EssStabl.ChannelId.TEMPERATURE_MIN_SYSTEM, new UnsignedWordElement(6000),
								SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.TEMPERATURE_MIN_STRING_1, new UnsignedWordElement(6001),
								SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.TEMPERATURE_MIN_STRING_1_MODULE_X, new UnsignedWordElement(6002),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6022, Priority.HIGH,
						m(EssStabl.ChannelId.TEMPERATURE_MIN_STRING_2, new UnsignedWordElement(6022),
								SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.TEMPERATURE_MIN_STRING_2_MODULE_X, new UnsignedWordElement(6023),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6043, Priority.HIGH,
						m(EssStabl.ChannelId.TEMPERATURE_MIN_STRING_3, new UnsignedWordElement(6043),
								SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.TEMPERATURE_MIN_STRING_3_MODULE_X, new UnsignedWordElement(6044),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6064, Priority.HIGH,
						m(EssStabl.ChannelId.TEMPERATURE_MAX_SYSTEM, new UnsignedWordElement(6064),
								SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.TEMPERATURE_MAX_STRING_1, new UnsignedWordElement(6065),
								SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.TEMPERATURE_MAX_STRING_1_MODULE_X, new UnsignedWordElement(6066),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6086, Priority.HIGH,
						m(EssStabl.ChannelId.TEMPERATURE_MAX_STRING_2, new UnsignedWordElement(6086),
								SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.TEMPERATURE_MAX_STRING_2_MODULE_X, new UnsignedWordElement(6087),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6107, Priority.HIGH,
						m(EssStabl.ChannelId.TEMPERATURE_MAX_STRING_3, new UnsignedWordElement(6107),
								SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.TEMPERATURE_MAX_STRING_3_MODULE_X, new UnsignedWordElement(6108),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6128, Priority.HIGH,
						m(EssStabl.ChannelId.TEMPERATURE_AVG_SYSTEM, new UnsignedWordElement(6128),
								SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.TEMPERATURE_AVG_STRING_1, new UnsignedWordElement(6129),
								SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.TEMPERATURE_AVG_STRING_1_MODULE_X, new UnsignedWordElement(6130),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6150, Priority.HIGH,
						m(EssStabl.ChannelId.TEMPERATURE_AVG_STRING_2, new UnsignedWordElement(6150),
								SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.TEMPERATURE_AVG_STRING_2_MODULE_X, new UnsignedWordElement(6151),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6171, Priority.HIGH,
						m(EssStabl.ChannelId.TEMPERATURE_AVG_STRING_3, new UnsignedWordElement(6171),
								SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.TEMPERATURE_AVG_STRING_3_MODULE_X, new UnsignedWordElement(6172),
								SCALE_FACTOR_MINUS_2)),
				// VOLTAGE
				new FC3ReadRegistersTask(6192, Priority.HIGH,
						m(EssStabl.ChannelId.VOLTAGE_MIN_SYSTEM, new UnsignedWordElement(6192), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.VOLTAGE_MIN_STRING_1, new UnsignedWordElement(6193), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.VOLTAGE_MIN_STRING_1_MODULE_X, new UnsignedWordElement(6194),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6214, Priority.HIGH,
						m(EssStabl.ChannelId.VOLTAGE_MIN_STRING_2, new UnsignedWordElement(6214), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.VOLTAGE_MIN_STRING_2_MODULE_X, new UnsignedWordElement(6215),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6235, Priority.HIGH,
						m(EssStabl.ChannelId.VOLTAGE_MIN_STRING_3, new UnsignedWordElement(6235), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.VOLTAGE_MIN_STRING_3_MODULE_X, new UnsignedWordElement(6236),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6256, Priority.HIGH,
						m(EssStabl.ChannelId.VOLTAGE_MAX_SYSTEM, new UnsignedWordElement(6256), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.VOLTAGE_MAX_STRING_1, new UnsignedWordElement(6257), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.VOLTAGE_MAX_STRING_1_MODULE_X, new UnsignedWordElement(6258),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6278, Priority.HIGH,
						m(EssStabl.ChannelId.VOLTAGE_MAX_STRING_2, new UnsignedWordElement(6278), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.VOLTAGE_MAX_STRING_2_MODULE_X, new UnsignedWordElement(6279),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6299, Priority.HIGH,
						m(EssStabl.ChannelId.VOLTAGE_MAX_STRING_3, new UnsignedWordElement(6299), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.VOLTAGE_MAX_STRING_3_MODULE_X, new UnsignedWordElement(6300),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6320, Priority.HIGH,
						m(EssStabl.ChannelId.VOLTAGE_AVG_SYSTEM, new UnsignedWordElement(6320), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.VOLTAGE_AVG_STRING_1, new UnsignedWordElement(6321), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.VOLTAGE_AVG_STRING_1_MODULE_X, new UnsignedWordElement(6322),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6342, Priority.HIGH,
						m(EssStabl.ChannelId.VOLTAGE_AVG_STRING_2, new UnsignedWordElement(6342), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.VOLTAGE_AVG_STRING_2_MODULE_X, new UnsignedWordElement(6343),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6363, Priority.HIGH,
						m(EssStabl.ChannelId.VOLTAGE_AVG_STRING_3, new UnsignedWordElement(6363), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.VOLTAGE_AVG_STRING_3_MODULE_X, new UnsignedWordElement(6364),
								SCALE_FACTOR_MINUS_2)),
				// CURRENT
				new FC3ReadRegistersTask(6384, Priority.HIGH,
						m(EssStabl.ChannelId.CURRENT_MIN_SYSTEM, new UnsignedWordElement(6384), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.CURRENT_MIN_STRING_1, new UnsignedWordElement(6385), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.CURRENT_MIN_STRING_1_MODULE_X, new UnsignedWordElement(6386),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6406, Priority.HIGH,
						m(EssStabl.ChannelId.CURRENT_MIN_STRING_2, new UnsignedWordElement(6406), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.CURRENT_MIN_STRING_2_MODULE_X, new UnsignedWordElement(6407),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6427, Priority.HIGH,
						m(EssStabl.ChannelId.CURRENT_MIN_STRING_3, new UnsignedWordElement(6427), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.CURRENT_MIN_STRING_3_MODULE_X, new UnsignedWordElement(6428),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6448, Priority.HIGH,
						m(EssStabl.ChannelId.CURRENT_MAX_SYSTEM, new UnsignedWordElement(6448), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.CURRENT_MAX_STRING_1, new UnsignedWordElement(6449), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.CURRENT_MAX_STRING_1_MODULE_X, new UnsignedWordElement(6450),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6470, Priority.HIGH,
						m(EssStabl.ChannelId.CURRENT_MAX_STRING_2, new UnsignedWordElement(6470), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.CURRENT_MAX_STRING_2_MODULE_X, new UnsignedWordElement(6471),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6491, Priority.HIGH,
						m(EssStabl.ChannelId.CURRENT_MAX_STRING_3, new UnsignedWordElement(6491), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.CURRENT_MAX_STRING_3_MODULE_X, new UnsignedWordElement(6492),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6512, Priority.HIGH,
						m(EssStabl.ChannelId.CURRENT_AVG_SYSTEM, new UnsignedWordElement(6512), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.CURRENT_AVG_STRING_1, new UnsignedWordElement(6513), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.CURRENT_AVG_STRING_1_MODULE_X, new UnsignedWordElement(6514),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6534, Priority.HIGH,
						m(EssStabl.ChannelId.CURRENT_AVG_STRING_2, new UnsignedWordElement(6534), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.CURRENT_AVG_STRING_2_MODULE_X, new UnsignedWordElement(6535),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6555, Priority.HIGH,
						m(EssStabl.ChannelId.CURRENT_AVG_STRING_3, new UnsignedWordElement(6555), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.CURRENT_AVG_STRING_3_MODULE_X, new UnsignedWordElement(6556),
								SCALE_FACTOR_MINUS_2)),
				// SOC
				new FC3ReadRegistersTask(6576, Priority.HIGH,
						m(EssStabl.ChannelId.SOC_MIN_SYSTEM, new UnsignedWordElement(6576), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOC_MIN_STRING_1, new UnsignedWordElement(6577), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOC_MIN_STRING_1_MODULE_X, new UnsignedWordElement(6578),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6598, Priority.HIGH,
						m(EssStabl.ChannelId.SOC_MIN_STRING_2, new UnsignedWordElement(6598), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOC_MIN_STRING_2_MODULE_X, new UnsignedWordElement(6599),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6619, Priority.HIGH,
						m(EssStabl.ChannelId.SOC_MIN_STRING_3, new UnsignedWordElement(6619), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOC_MIN_STRING_3_MODULE_X, new UnsignedWordElement(6620),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6640, Priority.HIGH,
						m(EssStabl.ChannelId.SOC_MAX_SYSTEM, new UnsignedWordElement(6640), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOC_MAX_STRING_1, new UnsignedWordElement(6641), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOC_MAX_STRING_1_MODULE_X, new UnsignedWordElement(6642),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6662, Priority.HIGH,
						m(EssStabl.ChannelId.SOC_MAX_STRING_2, new UnsignedWordElement(6662), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOC_MAX_STRING_2_MODULE_X, new UnsignedWordElement(6663),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6683, Priority.HIGH,
						m(EssStabl.ChannelId.SOC_MAX_STRING_3, new UnsignedWordElement(6683), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOC_MAX_STRING_3_MODULE_X, new UnsignedWordElement(6684),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6704, Priority.HIGH,
						m(SymmetricEss.ChannelId.SOC, new UnsignedWordElement(6704), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOC_AVG_STRING_1, new UnsignedWordElement(6705), SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6726, Priority.HIGH,
						m(EssStabl.ChannelId.SOC_AVG_STRING_2, new UnsignedWordElement(6726), SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6747, Priority.HIGH,
						m(EssStabl.ChannelId.SOC_AVG_STRING_3, new UnsignedWordElement(6747), SCALE_FACTOR_MINUS_2)),
				// SOH
				new FC3ReadRegistersTask(6768, Priority.HIGH,
						m(EssStabl.ChannelId.SOH_MIN_SYSTEM, new UnsignedWordElement(6768), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOH_MIN_STRING_1, new UnsignedWordElement(6769), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOH_MIN_STRING_1_MODULE_X, new UnsignedWordElement(6770),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6790, Priority.HIGH,
						m(EssStabl.ChannelId.SOH_MIN_STRING_2, new UnsignedWordElement(6790), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOH_MIN_STRING_2_MODULE_X, new UnsignedWordElement(6791),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6811, Priority.HIGH,
						m(EssStabl.ChannelId.SOH_MIN_STRING_3, new UnsignedWordElement(6811), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOH_MIN_STRING_3_MODULE_X, new UnsignedWordElement(6812),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6832, Priority.HIGH,
						m(EssStabl.ChannelId.SOH_MAX_SYSTEM, new UnsignedWordElement(6832), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOH_MAX_STRING_1, new UnsignedWordElement(6833), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOH_MAX_STRING_1_MODULE_X, new UnsignedWordElement(6834),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6854, Priority.HIGH,
						m(EssStabl.ChannelId.SOH_MAX_STRING_2, new UnsignedWordElement(6854), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOH_MAX_STRING_2_MODULE_X, new UnsignedWordElement(6855),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6875, Priority.LOW,
						m(EssStabl.ChannelId.SOH_MAX_STRING_3, new UnsignedWordElement(6875), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOH_MAX_STRING_3_MODULE_X, new UnsignedWordElement(6876),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6896, Priority.HIGH,
						m(EssStabl.ChannelId.SOH_AVG_SYSTEM, new UnsignedWordElement(6896), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOH_AVG_STRING_1, new UnsignedWordElement(6897), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOH_AVG_STRING_1_MODULE_X, new UnsignedWordElement(6898),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6918, Priority.HIGH,
						m(EssStabl.ChannelId.SOH_AVG_STRING_2, new UnsignedWordElement(6918), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOH_AVG_STRING_2_MODULE_X, new UnsignedWordElement(6919),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6939, Priority.HIGH,
						m(EssStabl.ChannelId.SOH_AVG_STRING_3, new UnsignedWordElement(6939), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.SOH_AVG_STRING_3_MODULE_X, new UnsignedWordElement(6940),
								SCALE_FACTOR_MINUS_2)),
				new FC3ReadRegistersTask(6960, Priority.HIGH,
						m(EssStabl.ChannelId.CURRENT_LIMIT_DISCHARGE_SYSTEM, new UnsignedWordElement(6960)),
						m(EssStabl.ChannelId.CURRENT_LIMIT_DISCHARGE_SYSTEM_1, new UnsignedWordElement(6961)),
						m(EssStabl.ChannelId.CURRENT_LIMIT_DISCHARGE_SYSTEM_2, new UnsignedWordElement(6962)),
						m(EssStabl.ChannelId.CURRENT_LIMIT_DISCHARGE_SYSTEM_3, new UnsignedWordElement(6963))),
				new FC3ReadRegistersTask(6964, Priority.HIGH,
						m(EssStabl.ChannelId.CURRENT_LIMIT_CHARGE_SYSTEM, new UnsignedWordElement(6964)),
						m(EssStabl.ChannelId.CURRENT_LIMIT_CHARGE_SYSTEM_1, new UnsignedWordElement(6965)),
						m(EssStabl.ChannelId.CURRENT_LIMIT_CHARGE_SYSTEM_2, new UnsignedWordElement(6966)),
						m(EssStabl.ChannelId.CURRENT_LIMIT_CHARGE_SYSTEM_3, new UnsignedWordElement(6967))),
				// TODO Add this later
				// new FC3ReadRegistersTask(6968, Priority.HIGH,
				// m(SymmetricEss.ChannelId.CAPACITY, new UnsignedWordElement(6968))),

				new FC4ReadInputRegistersTask(5000, Priority.HIGH,
						m(EssStabl.ChannelId.ACTUAL_MAIN_STATE, new SignedWordElement(5000)),
						m(EssStabl.ChannelId.NUMBER_OF_CONNECTED_STABLE_MODULES_1, new SignedWordElement(5001)),
						m(EssStabl.ChannelId.NUMBER_OF_CONNECTED_STABLE_MODULES_2, new SignedWordElement(5002)),
						m(EssStabl.ChannelId.NUMBER_OF_CONNECTED_STABLE_MODULES_3, new SignedWordElement(5003))),
				new FC4ReadInputRegistersTask(5023, Priority.LOW,
						m(EssStabl.ChannelId.AC_DC_ACTIVE_GRID_TYPE, new UnsignedWordElement(5023))),
				new FC4ReadInputRegistersTask(5140, Priority.HIGH,
						m(EssStabl.ChannelId.STABL_ACTIVE_POWER_L1, new SignedWordElement(5140)),
						m(EssStabl.ChannelId.STABL_ACTIVE_POWER_L2, new SignedWordElement(5141)),
						m(EssStabl.ChannelId.STABL_ACTIVE_POWER_L3, new SignedWordElement(5142))),
				new FC4ReadInputRegistersTask(5143, Priority.HIGH,
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L1, new SignedWordElement(5143), SCALE_FACTOR_MINUS_2),
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L2, new SignedWordElement(5144), SCALE_FACTOR_MINUS_2),
						m(AsymmetricEss.ChannelId.REACTIVE_POWER_L3, new SignedWordElement(5145),
								SCALE_FACTOR_MINUS_2)),
				new FC4ReadInputRegistersTask(5160, Priority.HIGH,
						m(EssStabl.ChannelId.GRID_VOLTAGE_L1, new SignedWordElement(5160), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.GRID_VOLTAGE_L2, new SignedWordElement(5161), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.GRID_VOLTAGE_L3, new SignedWordElement(5162), SCALE_FACTOR_MINUS_2)),
				new FC4ReadInputRegistersTask(5500, Priority.HIGH,
						m(EssStabl.ChannelId.INLET_AIR_TEMPERATURE, new SignedWordElement(5500), SCALE_FACTOR_MINUS_2),
						m(EssStabl.ChannelId.MCU_CORE_TEMPERATURE, new SignedWordElement(5501), SCALE_FACTOR_MINUS_2)),
				new FC4ReadInputRegistersTask(5600, Priority.LOW,
						m(EssStabl.ChannelId.MAINTAIN_MODE, new SignedWordElement(5600), SCALE_FACTOR_MINUS_2)),
				new FC4ReadInputRegistersTask(2401, Priority.LOW,
						m(EssStabl.ChannelId.TOTAL_WARNING_CNT, new SignedWordElement(2401))),
				new FC4ReadInputRegistersTask(2403, Priority.LOW, //
						m(EssStabl.ChannelId.WARNING1, new SignedWordElement(2403)),
						m(EssStabl.ChannelId.WARNING2, new SignedWordElement(2404)),
						m(EssStabl.ChannelId.WARNING3, new SignedWordElement(2405)),
						m(EssStabl.ChannelId.WARNING4, new SignedWordElement(2406)),
						m(EssStabl.ChannelId.WARNING5, new SignedWordElement(2407)),
						m(EssStabl.ChannelId.WARNING6, new SignedWordElement(2408)),
						m(EssStabl.ChannelId.WARNING7, new SignedWordElement(2409)),
						m(EssStabl.ChannelId.WARNING8, new SignedWordElement(2410)),
						m(EssStabl.ChannelId.WARNING9, new SignedWordElement(2411)),
						m(EssStabl.ChannelId.WARNING10, new SignedWordElement(2412)),
						m(EssStabl.ChannelId.WARNING11, new SignedWordElement(2413)),
						m(EssStabl.ChannelId.WARNING12, new SignedWordElement(2414)),
						m(EssStabl.ChannelId.WARNING13, new SignedWordElement(2415)),
						m(EssStabl.ChannelId.WARNING14, new SignedWordElement(2416)),
						m(EssStabl.ChannelId.WARNING15, new SignedWordElement(2417)),
						m(EssStabl.ChannelId.WARNING16, new SignedWordElement(2418)),
						m(EssStabl.ChannelId.WARNING17, new SignedWordElement(2419)),
						m(EssStabl.ChannelId.WARNING18, new SignedWordElement(2420)),
						m(EssStabl.ChannelId.WARNING19, new SignedWordElement(2421)),
						m(EssStabl.ChannelId.WARNING20, new SignedWordElement(2422))),
				new FC4ReadInputRegistersTask(2808, Priority.LOW,
						m(EssStabl.ChannelId.TOTAL_ALARMS_CNT, new SignedWordElement(2808))),
				new FC4ReadInputRegistersTask(2810, Priority.LOW, //
						m(EssStabl.ChannelId.ALARM1, new SignedWordElement(2810)),
						m(EssStabl.ChannelId.ALARM2, new SignedWordElement(2811)),
						m(EssStabl.ChannelId.ALARM3, new SignedWordElement(2812)),
						m(EssStabl.ChannelId.ALARM4, new SignedWordElement(2813)),
						m(EssStabl.ChannelId.ALARM5, new SignedWordElement(2814)),
						m(EssStabl.ChannelId.ALARM6, new SignedWordElement(2815)),
						m(EssStabl.ChannelId.ALARM7, new SignedWordElement(2816)),
						m(EssStabl.ChannelId.ALARM8, new SignedWordElement(2817)),
						m(EssStabl.ChannelId.ALARM9, new SignedWordElement(2818)),
						m(EssStabl.ChannelId.ALARM10, new SignedWordElement(2819)),
						m(EssStabl.ChannelId.ALARM11, new SignedWordElement(2820)),
						m(EssStabl.ChannelId.ALARM12, new SignedWordElement(2821)),
						m(EssStabl.ChannelId.ALARM13, new SignedWordElement(2822)),
						m(EssStabl.ChannelId.ALARM14, new SignedWordElement(2823)),
						m(EssStabl.ChannelId.ALARM15, new SignedWordElement(2824)),
						m(EssStabl.ChannelId.ALARM16, new SignedWordElement(2825)),
						m(EssStabl.ChannelId.ALARM17, new SignedWordElement(2826)),
						m(EssStabl.ChannelId.ALARM18, new SignedWordElement(2827)),
						m(EssStabl.ChannelId.ALARM19, new SignedWordElement(2828)),
						m(EssStabl.ChannelId.ALARM20, new SignedWordElement(2829))),

				// new FC3ReadRegistersTask(3000, Priority.LOW, //
				// m(EssStabl.ChannelId.NS_PROTECTION_ERROR_CODE, new
				// UnsignedWordElement(3000))),

				// new FC6WriteRegisterTask(3000, //
				// m(EssStabl.ChannelId.NS_PROTECTION_ERROR_CODE, new
				// UnsignedWordElement(3000))),

				// Writable registers

				new FC6WriteRegisterTask(1016, //
						m(EssStabl.ChannelId.COM_TIME_OUT_EMS, new UnsignedWordElement(1016))),
				new FC6WriteRegisterTask(1017, //
						m(EssStabl.ChannelId.SOFTWARE_RESET, new UnsignedWordElement(1017))),

				new FC6WriteRegisterTask(1018, //
						m(EssStabl.ChannelId.STANDBY_MODE, new UnsignedWordElement(1018))),

				new FC6WriteRegisterTask(4000, //
						m(EssStabl.ChannelId.ACTIVATE_POWER_STAGE, new UnsignedWordElement(4000))),
				new FC6WriteRegisterTask(4002, //
						m(EssStabl.ChannelId.RESET_ALARM_FLAG, new UnsignedWordElement(4002))),
				new FC6WriteRegisterTask(4009, //
						m(EssStabl.ChannelId.GRID_TYPE, new UnsignedWordElement(4009))),
				new FC6WriteRegisterTask(4010, //
						m(EssStabl.ChannelId.GRID_CODE, new UnsignedWordElement(4010))),
				new FC6WriteRegisterTask(4222, //
						m(EssStabl.ChannelId.ISLAND_FREQUENCY_OFFSET, new UnsignedWordElement(4222),
								SCALE_FACTOR_MINUS_3)),

				new FC6WriteRegisterTask(4195, //
						m(EssStabl.ChannelId.SYSTEM_SIGNED_POWER_SET_POINT_AC, new UnsignedWordElement(4195)))
		/*
		 * new FC6WriteRegisterTask(7000, //
		 * m(EssStabl.ChannelId.ACTIVE_POWER_M0D_SETPOINT_STRING_1_MODULEX, new
		 * UnsignedWordElement(7000))), new FC6WriteRegisterTask(7020, //
		 * m(EssStabl.ChannelId.ACTIVE_POWER_M0D_SETPOINT_STRING_2_MODULEX, new
		 * UnsignedWordElement(7020))), new FC6WriteRegisterTask(7040, //
		 * m(EssStabl.ChannelId.ACTIVE_POWER_M0D_SETPOINT_STRING_3_MODULEX, // new
		 * UnsignedWordElement(7040)))//
		 */ );
	}

	public record DynamicChannel(String prefix, Unit unit) {
	}

	private void initializePowerChannels(int stringNumber, int baseAddress, int moduleCount, String prefix) {

		log.info("initializePowerChannels: String {}, found {} modules", stringNumber, moduleCount);

		List<ModbusElement> writeElements = new ArrayList<>();
		var dynamicChannel = new DynamicChannel(prefix, Unit.WATT);

		for (int moduleIndex = 0; moduleIndex < moduleCount; moduleIndex++) {

			var channelId = prefix + moduleIndex;
			int registerAddress = baseAddress + moduleIndex; // 1 register per module

			log.debug("Creating POWER channel: {} at register {}", channelId, registerAddress);

			writeElements.add(m(
					this.generateModuleChannel(channelId, INTEGER,
							c -> c.unit(dynamicChannel.unit()).accessMode(AccessMode.WRITE_ONLY)),
					new UnsignedWordElement(registerAddress)));
		}

		if (!writeElements.isEmpty()) {
			log.info("Adding {} power write elements to FC16 batch @ address {}", writeElements.size(), baseAddress);
			this.addModbusWriteTask(baseAddress, writeElements.toArray(ModbusElement[]::new));
		}
	}

	protected void initializeAllChannelsModule1() {
		var count = this.getNumberOfConnectedStableModules1();
		if (!count.isDefined() || count.get() <= this.string1InitializedCount) {
			return;
		}
		initializePowerChannels(1, 7000, count.get(), "ACTIVE_POWER_M0D_SETPOINT_STRING_1_MODULE_");
		initializeSocChannels(1, 6705, count.get(), "SOC_AVG_STRING_1_MODULE_");
		initializeTemperatureChannels(1, 6129, count.get(), "TEMPERATURE_AVG_STRING_1_MODULE_");
		this.string1InitializedCount = count.get();
	}

	protected void initializeAllChannelsModule2() {
		var count = this.getNumberOfConnectedStableModules2();
		if (!count.isDefined() || count.get() <= this.string2InitializedCount) {
			return;
		}
		initializePowerChannels(2, 7020, count.get(), "ACTIVE_POWER_M0D_SETPOINT_STRING_2_MODULE_");
		initializeSocChannels(2, 6726, count.get(), "SOC_AVG_STRING_2_MODULE_");
		initializeTemperatureChannels(2, 6150, count.get(), "TEMPERATURE_AVG_STRING_2_MODULE_");
		this.string2InitializedCount = count.get();
	}

	protected void initializeAllChannelsModule3() {
		var count = this.getNumberOfConnectedStableModules3();
		if (!count.isDefined() || count.get() <= this.string3InitializedCount) {
			return;
		}
		initializePowerChannels(3, 7040, count.get(), "ACTIVE_POWER_M0D_SETPOINT_STRING_3_MODULE_");
		initializeSocChannels(3, 6747, count.get(), "SOC_AVG_STRING_3_MODULE_");
		initializeTemperatureChannels(3, 6171, count.get(), "TEMPERATURE_AVG_STRING_3_MODULE_");
		this.string3InitializedCount = count.get();
	}

	private void addModbusWriteTask(int baseCellElementAddress, ModbusElement... elements) {
		this.getModbusProtocol()//
				.addTask(new FC16WriteRegistersTask(baseCellElementAddress, elements));
	}

	private void addModbusReadTask(int baseCellElementAddress, ModbusElement... elements) {
		this.getModbusProtocol()//
				.addTask(new FC3ReadRegistersTask(baseCellElementAddress, Priority.HIGH, elements));
	}

	private void initializeSocChannels(int stringNumber, int baseAddress, int moduleCount, String prefix) {

		var readElements = new ArrayList<>();
		var dynamicSocChannel = new DynamicChannel(prefix, Unit.PERCENT);
		for (int moduleIndex = 0; moduleIndex < moduleCount; moduleIndex++) {
			String channelId = prefix + moduleIndex;
			int registerAddress = baseAddress + moduleIndex + 1;

			readElements.add(m(
					this.generateModuleChannel(channelId, INTEGER,
							c -> c.unit(dynamicSocChannel.unit()).accessMode(AccessMode.READ_ONLY)),
					new UnsignedWordElement(registerAddress), SCALE_FACTOR_MINUS_2));
		}

		if (!readElements.isEmpty()) {
			this.addModbusReadTask(baseAddress + 1, readElements.toArray(ModbusElement[]::new));
		}
	}

	private void initializeTemperatureChannels(int stringNumber, int baseAddress, int moduleCount, String prefix) {

		var readElements = new ArrayList<>();
		var dynamicTempChannel = new DynamicChannel(prefix, Unit.DEGREE_CELSIUS);
		for (int moduleIndex = 0; moduleIndex < moduleCount; moduleIndex++) {
			String channelId = prefix + moduleIndex;
			int registerAddress = baseAddress + moduleIndex + 1;

			readElements.add(m(
					this.generateModuleChannel(channelId, INTEGER,
							c -> c.unit(dynamicTempChannel.unit()).accessMode(AccessMode.READ_ONLY)),
					new UnsignedWordElement(registerAddress), SCALE_FACTOR_MINUS_2));
		}

		if (!readElements.isEmpty()) {
			this.addModbusReadTask(baseAddress + 1, readElements.toArray(ModbusElement[]::new));
		}
	}

	private ChannelIdImpl generateModuleChannel(String channelName, OpenemsType openemsType,
			Consumer<OpenemsTypeDoc<?>> additionalDocConfig) {
		final var doc = Doc.of(openemsType);
		if (additionalDocConfig != null) {
			additionalDocConfig.accept(doc);
		}
		var channelId = new ChannelIdImpl(channelName, doc);
		this.addChannel(channelId);
		return channelId;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			this.stateMachine.run(this.config.essState());
			break;
		}
	}

	@Override
	public String debugLog() {
		return "SoC:" + this.getSoc().asString() //
				+ "|L:" + this.getActivePower().asString() //
				+ "|Allowed:" + this.getAllowedChargePower().asStringWithoutUnit() + ";" //
				+ this.getAllowedDischargePower().asString() //
				+ "|" + this.getGridModeChannel().value().asOptionString();
	}

	@Override
	public Power getPower() {
		return this.power;
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(EssStablImpl.class, accessMode, 100) //
						.build());
	}

	/**
	 * Gets a channel value by channel ID string.
	 */
	public Value<Integer> getValueFromChannelById(String channelId) {
		try {
			var channel = this.channel(new ChannelIdImpl(channelId, null));
			if (channel instanceof IntegerReadChannel readChannel) {
				return readChannel.value();
			}
		} catch (Exception e) {
			log.debug("Channel {} not found or not accessible: {}", channelId, e.getMessage());
		}
		return null;
	}

	@Override
	public void applyPower(int activePower, int reactivePower) throws OpenemsNamedException {
		if (!this.stateMachine.isRunning()) {
			return;
		}

		if (this.config.useExternalSorting()) {
			this.applySplitRanks(activePower);
		}

		IntegerWriteChannel setActivePower = this.channel(EssStabl.ChannelId.SYSTEM_SIGNED_POWER_SET_POINT_AC);
		setActivePower.setNextWriteValue((int) (activePower / EssStableConstants.POWER_SCALING_FACTOR));
	}

	private void applySplitRanks(int activePower) throws OpenemsNamedException {
		int[] connectedModules = this.getConnectedModuleCounts();
		if (this.calculateTotalConnectedModules(connectedModules) <= 0) {
			return;
		}
		int[][] ranks = this.httpPollEssSolver.calculateOptimalWeights(this, activePower, this.httpBridge,
				connectedModules, this.config.numberOfStrings(), this.config.solverUrl());
		this.applyRanksToModules(ranks, connectedModules);
	}

	/**
	 * Writes rank values directly to module registers. The Stabl EMS distributes
	 * power internally based on these ranks.
	 */
	private void applyRanksToModules(int[][] ranks, int[] connectedModules) throws OpenemsNamedException {
		for (int stringIndex = 0; stringIndex < EssStableConstants.NUMBER_OF_STRINGS; stringIndex++) {
			int modulesInString = connectedModules[stringIndex];

			for (int moduleIndex = 0; moduleIndex < modulesInString; moduleIndex++) {
				int rank = ranks[stringIndex][moduleIndex];
				try {
					String channelId = this.buildModuleChannelId(stringIndex + 1, moduleIndex);
					IntegerWriteChannel moduleChannel = this.channel(new ChannelIdImpl(channelId, null));
					moduleChannel.setNextWriteValue(rank);
				} catch (OpenemsNamedException e) {
					log.error("Failed to set rank for string {} module {}: {}", stringIndex + 1, moduleIndex,
							e.getMessage());
					throw e;
				}
			}

			log.info("String {} ranks set: {}", stringIndex + 1, java.util.Arrays.toString(ranks[stringIndex]));
		}
	}

	private int[] getConnectedModuleCounts() {
		return new int[] { //
				this.getNumberOfConnectedStableModules1().orElse(0), //
				this.getNumberOfConnectedStableModules2().orElse(0), //
				this.getNumberOfConnectedStableModules3().orElse(0) //
		};
	}

	/**
	 * Calculates total number of connected modules across all strings.
	 */
	private int calculateTotalConnectedModules(int[] connectedModules) {
		int total = 0;
		for (int modules : connectedModules) {
			total += modules;
		}
		return total;
	}

	/**
	 * Builds the channel ID for a specific module.
	 */
	private String buildModuleChannelId(int stringNumber, int moduleIndex) {
		return EssStableConstants.MODULE_CHANNEL_PREFIX + stringNumber + EssStableConstants.MODULE_CHANNEL_SUFFIX
				+ moduleIndex;
	}

	public void setPowerStage(int value) {
		IntegerWriteChannel powerStage = this.channel(EssStabl.ChannelId.ACTIVATE_POWER_STAGE);
		try {
			powerStage.setNextWriteValue(value);
		} catch (OpenemsNamedException e) {
			log.error("Failed to set power stage to {}: {}", value, e.getMessage());
		}
	}

	public void setSoftwareReset(int value) {
		IntegerWriteChannel softwareReset = this.channel(EssStabl.ChannelId.SOFTWARE_RESET);
		try {
			softwareReset.setNextWriteValue(value);
		} catch (OpenemsNamedException e) {
			log.error("Failed to set software reset to {}: {}", value, e.getMessage());
		}
	}

	@Override
	public SinglePhase getPhase() {
		return this.singlePhase;
	}
}