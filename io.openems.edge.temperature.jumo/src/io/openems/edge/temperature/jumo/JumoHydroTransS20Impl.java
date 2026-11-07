package io.openems.edge.temperature.jumo;

import static io.openems.edge.bridge.modbus.api.ModbusUtils.readElementOnce;
import static io.openems.edge.bridge.modbus.api.ModbusUtils.FunctionCode.FC3;
import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.bridge.modbus.api.AbstractOpenemsModbusComponent;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.ModbusProtocol;
import io.openems.edge.bridge.modbus.api.ModbusUtils;
import io.openems.edge.bridge.modbus.api.element.FloatDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.taskmanager.Priority;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Jumo.Hydro.Trans.S20", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("Modbus")
public class JumoHydroTransS20Impl extends AbstractOpenemsModbusComponent
		implements JumoHydroTransS20, OpenemsComponent, ModbusComponent {

	private final Logger log = LoggerFactory.getLogger(JumoHydroTransS20Impl.class);

	@Override
	@Reference(//
			policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.modbus_id})(enabled=true))")
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	public JumoHydroTransS20Impl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				JumoHydroTransS20.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId());
		this.setUnits();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ModbusProtocol defineModbusProtocol() {
		return new ModbusProtocol(this, //
				new FC16WriteRegistersTask(1001, //
						m(JumoHydroTransS20.ChannelId.TEMPERATURE_UNIT, new UnsignedWordElement(1001)),
						m(JumoHydroTransS20.ChannelId.HUMIDITY_UNIT, new UnsignedWordElement(1002))),

				new FC3ReadRegistersTask(6000, Priority.LOW, //
						m(JumoHydroTransS20.ChannelId.RELATIVE_HUMIDITY, new FloatDoublewordElement(6000)),
						m(JumoHydroTransS20.ChannelId.TEMPERATURE, new FloatDoublewordElement(6002)),
						m(JumoHydroTransS20.ChannelId.CARBON_DIOXIDE, new FloatDoublewordElement(6004)),
						m(JumoHydroTransS20.ChannelId.DEW_POINT, new FloatDoublewordElement(6006)),
						m(JumoHydroTransS20.ChannelId.MIXING_RATION, new FloatDoublewordElement(6008)),
						m(JumoHydroTransS20.ChannelId.ABSOLUTE_HUMIDITY, new FloatDoublewordElement(6010)),
						m(JumoHydroTransS20.ChannelId.SPECIFIC_ENTHALPY, new FloatDoublewordElement(6012)),
						m(JumoHydroTransS20.ChannelId.WET_BULB_TEMPERATURE, new FloatDoublewordElement(6014)),
						m(JumoHydroTransS20.ChannelId.FROST_POINT, new FloatDoublewordElement(6016)),
						m(JumoHydroTransS20.ChannelId.PARTIAL_WATER_VAPOR_PRESSURE, new FloatDoublewordElement(6018))),

				new FC3ReadRegistersTask(6400, Priority.LOW, //
						m(JumoHydroTransS20.ChannelId.GENERAL_ERROR, new UnsignedWordElement(6400)),
						m(JumoHydroTransS20.ChannelId.CONFIGURATION_FAULT, new UnsignedWordElement(6401)),
						m(JumoHydroTransS20.ChannelId.CALIBRATION_FAULT, new UnsignedWordElement(6402)),
						m(JumoHydroTransS20.ChannelId.DEVICE_NOT_CALIBRATED, new UnsignedWordElement(6403)),
						m(JumoHydroTransS20.ChannelId.HUMIDITY_SENSOR_FAULTY, new UnsignedWordElement(6404)),
						m(JumoHydroTransS20.ChannelId.CO2_SENSOR_FAULTY, new UnsignedWordElement(6405)))//
		);
	}

	private void setUnits() {
		readElementOnce(FC3, this.getModbusProtocol(), ModbusUtils::retryOnNull, new UnsignedWordElement(1001))
				.thenAccept(value -> {
					if (value == null) {
						return;
					}
					try {
						setWriteValueIfNotRead(this.channel(JumoHydroTransS20.ChannelId.TEMPERATURE_UNIT),
								TemperatureUnit.DEGREE_CELSIUS);
						setWriteValueIfNotRead(this.channel(JumoHydroTransS20.ChannelId.HUMIDITY_UNIT),
								HumidityUnit.GRAM_PER_CUBIC_METER);
					} catch (OpenemsNamedException e) {
						this.logWarn(this.log, "Unable to set value [" + value + "] " + e.getMessage());
					}
				});
	}
}
