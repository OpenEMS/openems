package io.openems.edge.deye.dccharger;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;
import io.openems.edge.common.taskmanager.Priority;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
// ToDo import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Deye.DcCharger", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE //
})
public class DeyeDcChargerImpl extends AbstractOpenemsModbusComponent implements EssDcCharger, DeyeDcCharger,
		ModbusComponent, OpenemsComponent, EventHandler, ModbusSlave, TimedataProvider {

	private final SetPvLimitHandler setPvLimitHandler = new SetPvLimitHandler(this,
			ManagedSymmetricPvInverter.ChannelId.ACTIVE_POWER_LIMIT);

	// ToDo
	// private final CalculateEnergyFromPower calculateProductionEnergy = new
	// CalculateEnergyFromPower(this,
	// EssDcCharger.ChannelId.ACTUAL_ENERGY);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	private final Logger log = LoggerFactory.getLogger(DeyeDcChargerImpl.class);

	protected Config config;

	public DeyeDcChargerImpl() throws OpenemsException {
		super(//

				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				DeyeDcCharger.ChannelId.values() //

		);
		// DeyeDcCharger.calculateSumActivePowerFromPhases(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		// this._setMaxApparentPower(config.maxActivePower());

		// Stop if component is disabled
		if (!config.enabled()) {
			return;
		}

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC3ReadRegistersTask(667, Priority.LOW, //
						m(DeyeDcCharger.ChannelId.ACTIVE_POWER_GENERATOR, new UnsignedWordElement(667)),
						new DummyRegisterElement(668, 671),

						m(DeyeDcCharger.ChannelId.DC_POWER_STRING_1, new UnsignedWordElement(672)),
						m(DeyeDcCharger.ChannelId.DC_POWER_STRING_2, new UnsignedWordElement(673)),
						m(DeyeDcCharger.ChannelId.DC_POWER_STRING_3, new UnsignedWordElement(674)),
						m(DeyeDcCharger.ChannelId.DC_POWER_STRING_4, new UnsignedWordElement(675)),

						m(DeyeDcCharger.ChannelId.DC_VOLTAGE_STRING_1, new UnsignedWordElement(676),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(DeyeDcCharger.ChannelId.DC_CURRENT_STRING_1, new UnsignedWordElement(677),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(DeyeDcCharger.ChannelId.DC_VOLTAGE_STRING_2, new UnsignedWordElement(678),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(DeyeDcCharger.ChannelId.DC_CURRENT_STRING_2, new UnsignedWordElement(679),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(DeyeDcCharger.ChannelId.DC_VOLTAGE_STRING_3, new UnsignedWordElement(680),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(DeyeDcCharger.ChannelId.DC_CURRENT_STRING_3, new UnsignedWordElement(681),
								ElementToChannelConverter.SCALE_FACTOR_2),

						m(DeyeDcCharger.ChannelId.DC_VOLTAGE_STRING_4, new UnsignedWordElement(682),
								ElementToChannelConverter.SCALE_FACTOR_2),
						m(DeyeDcCharger.ChannelId.DC_CURRENT_STRING_4, new UnsignedWordElement(683),
								ElementToChannelConverter.SCALE_FACTOR_2)

				));
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			try {
				this.setPvLimitHandler.run();

				this.channel(DeyeDcCharger.ChannelId.PV_LIMIT_FAILED).setNextValue(false);
			} catch (OpenemsError.OpenemsNamedException e) {
				this.channel(DeyeDcCharger.ChannelId.PV_LIMIT_FAILED).setNextValue(true);
			}
			break;
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			// this.calculateProductionEnergy.update(this.getActivePower().get());
			this.calculateAndSetActualValues();
			break;
			
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			// this.calculateProductionEnergy.update(this.getActivePower().get());
			//this.calculateActualPower();
			break;			
		}
	}

	public String collectDebugData() {
		// Collect channel values in one stream
		return Stream.of(OpenemsComponent.ChannelId.values(), //
				EssDcCharger.ChannelId.values(), //
				ManagedSymmetricPvInverter.ChannelId.values(), //
				DeyeDcCharger.ChannelId.values() //
		).flatMap(Arrays::stream).map(id -> {
			try {
				return id.name() + "=" + this.channel(id).value().asString();
			} catch (Exception e) {
				return id.name() + "=n/a";
			}
		}).collect(Collectors.joining("; \n"));
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	protected void logDebug() {
		if (this.config.debugMode()) {

			if (this.config.extendedDebugMode()) {
				this.logInfo(this.log,
						"\n ############################################## Meter Values Start #############################################");
				this.logInfo(log, this.collectDebugData());
				this.logInfo(log,
						"\n ############################################## Meter Values End #############################################");

			}

		}
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				EssDcCharger.getModbusSlaveNatureTable(accessMode), //
				ManagedSymmetricPvInverter.getModbusSlaveNatureTable(accessMode));
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	public Integer getPvProduction() {
		return this.getActualPower().get();
	}

	private void calculateAndSetActualValues() {

		// power sum DC Strings
		Integer totalPower;
		try {
			totalPower = TypeUtils.sum(this.getDcPowerString1().getOrError(), this.getDcPowerString2().getOrError(),
					this.getDcPowerString3().getOrError(), this.getDcPowerString4().getOrError());
			this._setActualPower(totalPower);
		} catch (OpenemsNamedException e) {
			this.logError(this.log, "Unable to get DcCharger values " + e.getMessage());
		}
		
	}

	@Override
	public boolean hasError() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean hasWarning() {
		// TODO Auto-generated method stub
		return false;
	}
}
