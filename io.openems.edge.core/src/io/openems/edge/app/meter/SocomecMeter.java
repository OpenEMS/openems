package io.openems.edge.app.meter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.TreeMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.meter.SocomecMeter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
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
 * Describes a App for a Socomec meter.
 *
 * <pre>
  {
    "appId":"App.Meter.Socomec",
    "alias":"Socomec Zähler",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"METER_ID": "meter1",
    	"TYPE": "PRODUCTION",
    	"MODBUS_UNIT_ID": 6
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems/fems-app-socomec-zaehler-2">https://fenecon.de/fems/fems-app-socomec-zaehler-2</a>
    }
  }
 * </pre>
 */
@Component(name = "App.Meter.Socomec")
public class SocomecMeter extends AbstractMeterApp<Property> implements OpenemsApp {

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
	public SocomecMeter(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			// modbus id for connection to battery-inverter for a HOME
			var modbusId = "modbus1";
			var meterId = this.getId(t, p, Property.METER_ID, "meter1");

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			var type = this.getValueOrDefault(p, Property.TYPE, "PRODUCTION");
			var modbusUnitId = EnumUtils.getAsInt(p, Property.MODBUS_UNIT_ID);

			var components = new ArrayList<EdgeConfig.Component>();

			components.add(new EdgeConfig.Component(meterId, alias, "Meter.Socomec.Threephase", //
					JsonUtils.buildJsonObject() //
							.addProperty("modbus.id", modbusId) //
							.addProperty("modbusUnitId", modbusUnitId) //
							.addProperty("type", type) //
							.build()));

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.TYPE) //
								.setLabel(bundle.getString("App.Meter.mountType.label")) //
								.setOptions(this.buildMeterOptions(language)) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.MODBUS_UNIT_ID) //
								.setLabel(bundle.getString("modbusUnitId")) //
								.setDescription(bundle.getString("modbusUnitId.description")) //
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
				.setWebsiteUrl("https://fenecon.de/fems/fems-app-socomec-zaehler-2") //
				.build();
	}

	@Override
	public Builder getValidateBuilder() {
		return Validator.create() //
				.setCompatibleCheckableConfigs(Lists.newArrayList(//
						new Validator.CheckableConfig(CheckHome.COMPONENT_NAME,
								new Validator.MapBuilder<>(new TreeMap<String, Object>()) //
										.build())));
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAYAAAB5fY51AAABhWlDQ1BJQ0MgUHJvZmlsZQAAKM+VkT1Iw"
				+ "1AUhU9TpVIqgu0g4pChOlkQFXGUKBbBQmkrtOpg8tI/aNKQpLg4Cq4FB38Wqw4uzro6uAqC4A+Io5OToouUeF9SaBEqeOHxPs575"
				+ "3DffYDQqDDN6pkANN02U3FJzOZWxcArfAghiEGEZWYZifRiBl3r655uU93FeBb+V/1q3mKATySeY4ZpE28Qz2zaBud94ggrySrxO"
				+ "fG4SQ0SP3Jd8fiNc9FlgWdGzExqnjhCLBY7WOlgVjI14mniqKrplC9kPVY5b3HWKjXW6pO/MJTXV9JcpzWCOJaQQBIiFNRQRgU2Y"
				+ "rTrpFhI0bnUxT/s+pPkUshVBiPHAqrQILt+8D/4PVurMDXpJYUkoPfFcT5GgcAu0Kw7zvex4zRPAP8zcKW3/dUGMPtJer2tRY+Ag"
				+ "W3g4rqtKXvA5Q4w9GTIpuxKflpCoQC8n9E35YDwLRBc8+bWOsfpA5ChWS3fAAeHwFiRste7vLuvc25/3nHnB+kHJSZyiKHoIYQAA"
				+ "AAJcEhZcwAADsQAAA7EAZUrDhsAAD5+SURBVHhe7Z0HfBTXtf8P6r0XUBeIJlFELwZjDLjiktixEyeOE6c69dl5yT/1H8dOeZ/kx"
				+ "UnsvBTnxbHjluBeANNseq+iCARCFfVeUNe8+zu7I0bLjrQSKrva80XD3p29U3bm3t+ec+YWEgRBEARBEARBEARBEARBEARBEARBE"
				+ "ARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBE"
				+ "ARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBE"
				+ "ARBEARBEARBEARBEARBEARBEARBsDDO+ioIQ86hc0XaS9uOUlxkCH36xjmUEB0m5U24JqQACUPOxdJq7W8bDtA/Nh2mstpGLmSZk"
				+ "+LosXuupwfXzJMyJwwaKTzCkPLs+v3a02/voVP5ZVy6xo3zINI09aeRj5cn3TRvCn3n3utpZWaalD1hwEihEYaEjQfPar97cxd9d"
				+ "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";
	}

}
