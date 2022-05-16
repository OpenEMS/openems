package io.openems.edge.app.meter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.TreeMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingBiFunction;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.meter.CarloGavazziMeter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.validator.CheckHome;
import io.openems.edge.core.appmanager.validator.Validator;
import io.openems.edge.core.appmanager.validator.Validator.Builder;

/**
 * Describes a app for a Carlo Gavazzi meter.
 *
 * <pre>
  {
    "appId":"App.Meter.CarloGavazzi",
    "alias":"Carlo Gavazzi Zähler",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"METER_ID": "meter1",
    	"TYPE": "PRODUCTION",
    	"MODBUS_UNIT_ID": 6
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-carlo-gavazzi-zaehler-2/">https://fenecon.de/fems-2-2/fems-app-carlo-gavazzi-zaehler-2/</a>
    }
  }
 * </pre>
 */
@Component(name = "App.Meter.CarloGavazzi")
public class CarloGavazziMeter extends AbstractMeterApp<Property> implements OpenemsApp {

	public enum Property {
		// Components
		METER_ID, //
		// User-Values
		ALIAS, //
		TYPE, //
		MODBUS_UNIT_ID, //
		;
	}

	@Activate
	public CarloGavazziMeter(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingBiFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p) -> {

			// modbus id for connection to battery-inverter for a HOME
			var modbusId = "modbus1";
			var meterId = this.getId(t, p, Property.METER_ID, "meter1");

			var alias = this.getValueOrDefault(p, Property.ALIAS, "PV");
			var type = this.getValueOrDefault(p, Property.TYPE, "PRODUCTION");

			var modbusUnitId = EnumUtils.getAsInt(p, Property.MODBUS_UNIT_ID);

			var components = new ArrayList<EdgeConfig.Component>();

			components.add(new EdgeConfig.Component(meterId, alias, "Meter.CarloGavazzi.EM300", //
					JsonUtils.buildJsonObject() //
							.addProperty("modbus.id", modbusId) //
							.addProperty("modbusUnitId", modbusUnitId) //
							.addProperty("type", type) //
							.build()));

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant() {
		return AppAssistant.create(this.getName()) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.TYPE) //
								.setLabel("Mount Type") //
								.setOptions(this.buildMeterOptions()) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.MODBUS_UNIT_ID) //
								.setLabel("Modbus Unit-ID") //
								.setDescription("The Unit-ID of the Modbus device.") //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(6) //
								.setMin(0) //
								.isRequired(true) //
								.build()) //
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public Builder getValidateBuilder() {
		return Validator.create() //
				.setCompatibleCheckableNames(new Validator.MapBuilder<>(new TreeMap<String, Map<String, ?>>()) //
						.put(CheckHome.COMPONENT_NAME, //
								new Validator.MapBuilder<>(new TreeMap<String, Object>()) //
										.build())
						.build());
	}

	@Override
	public String getImage() {
		return OpenemsApp.FALLBACK_IMAGE;
	}

	@Override
	public String getName() {
		return "Carlo Gavazzi Zähler";
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

}
