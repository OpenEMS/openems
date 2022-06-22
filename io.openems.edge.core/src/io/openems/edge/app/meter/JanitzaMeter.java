package io.openems.edge.app.meter;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import io.openems.edge.app.meter.JanitzaMeter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Validation;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.validator.CheckHome;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

/**
 * Describes a App for a Janitza meter.
 *
 * <pre>
  {
    "appId":"App.Meter.Janitza",
    "alias":"Janitza Zähler",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"METER_ID": "meter1",
    	"MODBUS_ID": "modbus2",
    	"TYPE": "PRODUCTION",
    	"MODEL": "Meter.Janitza.UMG96RME",
    	"IP": "10.4.0.12",
    	"MODBUS_UNIT_ID": 1
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-janitza-zaehler-2/">https://fenecon.de/fems-2-2/fems-app-janitza-zaehler-2/</a>
    }
  }
 * </pre>
 */
@Component(name = "App.Meter.Janitza")
public class JanitzaMeter extends AbstractMeterApp<Property> implements OpenemsApp {

	public enum Property {
		// Components
		METER_ID, //
		MODBUS_ID, //
		// User-Values
		ALIAS, //
		MODEL, //
		TYPE, //
		IP, //
		MODBUS_UNIT_ID, //
		;
	}

	@Activate
	public JanitzaMeter(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var meterId = this.getId(t, p, Property.METER_ID, "meter1");

			// TODO which modbus should be used(new or already existing from home) only one
			// meter installed so far.

			// modbus id for connection to battery-inverter for a HOME
			// var modbusId = "modbus1";
			var modbusId = this.getId(t, p, Property.MODBUS_ID, "modbus2");

			var alias = this.getValueOrDefault(p, Property.ALIAS, this.getName(l));
			var factorieId = this.getValueOrDefault(p, Property.MODEL, "Meter.Janitza.UMG96RME");
			var type = this.getValueOrDefault(p, Property.TYPE, "PRODUCTION");
			var ip = this.getValueOrDefault(p, Property.IP, "10.4.0.12");
			var modbusUnitId = EnumUtils.getAsInt(p, Property.MODBUS_UNIT_ID);

			var components = new ArrayList<EdgeConfig.Component>();

			components.add(new EdgeConfig.Component(meterId, alias, factorieId, //
					JsonUtils.buildJsonObject() //
							.addProperty("modbus.id", modbusId) //
							.addProperty("modbusUnitId", modbusUnitId) //
							.addProperty("type", type) //
							.build()));

			components.add(new EdgeConfig.Component(modbusId, "bridge", "Bridge.Modbus.Tcp", //
					JsonUtils.buildJsonObject() //
							.addProperty("ip", ip) //
							.build()));

			return new AppConfiguration(components);
		};
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildSelect(Property.MODEL) //
								.setLabel(TranslationUtil.getTranslation(bundle, this.getAppId() + ".productModel")) //
								.isRequired(true) //
								.setOptions(this.buildFactorieIdOptions()) //
								.build()) //
						.add(JsonFormlyUtil.buildSelect(Property.TYPE) //
								.setLabel(TranslationUtil.getTranslation(bundle, "App.Meter.mountType.label")) //
								.isRequired(true) //
								.setOptions(this.buildMeterOptions(language)) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.IP) //
								.setLabel(TranslationUtil.getTranslation(bundle, "ipAddress")) //
								.setDescription(TranslationUtil.getTranslation(bundle, "App.Meter.ip.description")) //
								.isRequired(true) //
								.setDefaultValue("10.4.0.12") //
								.setValidation(Validation.IP) //
								.build())
						.add(JsonFormlyUtil.buildInput(Property.MODBUS_UNIT_ID) //
								.setLabel(TranslationUtil.getTranslation(bundle, "modbusUnitId")) //
								.setDescription(TranslationUtil.getTranslation(bundle, "modbusUnitId.description")) //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(1) //
								.setMin(0) //
								.isRequired(true) //
								.build()) //
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-janitza-zaehler-2/") //
				.build();
	}

	@Override
	public ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setCompatibleCheckableConfigs(Lists.newArrayList(//
						new ValidatorConfig.CheckableConfig(CheckHome.COMPONENT_NAME,
								new ValidatorConfig.MapBuilder<>(new TreeMap<String, Object>()) //
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

	protected final Set<Entry<String, String>> buildFactorieIdOptions() {
		var values = new HashSet<Entry<String, String>>();
		values.add(Map.entry("Janitza Netzanalysator UMG 96RM-E", "Meter.Janitza.UMG96RME"));
		values.add(Map.entry("Janitza Netzanalysator UMG 604-PRO", "Meter.Janitza.UMG604"));
		values.add(Map.entry("Janitza Netzqualitätsanalysator UMG 511", "Meter.Janitza.UMG511"));
		return values;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAYAAAB5fY51AAABhWlDQ1BJQ0MgUHJvZmlsZQAAKM+VkT1Iw"
				+ "1AUhU9TpVIqgu0g4pChOlkQFXGUKBbBQmkrtOpg8tI/aNKQpLg4Cq4FB38Wqw4uzro6uAqC4A+Io5OToouUeF9SaBEqeOHxPs575"
				+ "3DffYDQqDDN6pkANN02U3FJzOZWxcArfAghiEGEZWYZifRiBl3r655uU93FeBb+V/1q3mKATySeY4ZpE28Qz2zaBud94ggrySrxO"
				+ "fG4SQ0SP3Jd8fiNc9FlgWdGzExqnjhCLBY7WOlgVjI14mniqKrplC9kPVY5b3HWKjXW6pO/MJTXV9JcpzWCOJaQQBIiFNRQRgU2Y"
				+ "rTrpFhI0bnUxT/s+pPkUshVBiPHAqrQILt+8D/4PVurMDXpJYUkoPfFcT5GgcAu0Kw7zvex4zRPAP8zcKW3/dUGMPtJer2tRY+Ag"
				+ "W3g4rqtKXvA5Q4w9GTIpuxKflpCoQC8n9E35YDwLRBc8+bWOsfpA5ChWS3fAAeHwFiRste7vLuvc25/3nHnB+kHJSZyiKHoIYQAA"
				+ "AAJcEhZcwAADsQAAA7EAZUrDhsAADylSURBVHhe7Z0HfBTXtf8P6r03JKECokkU0YuxMQZsbGM7iVviFKf3npee/P+JnfI+yUt5T"
				+ "l7iOM+xHbfE3WDANBsDpvcmEE0N9V5Q17z7O7sjRsuOtKu65XzRsHdmp+3Mvb8558wtJAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCI"
				+ "AiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCI"
				+ "AiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCIAiCI"
				+ "AiCIAiCIAiCIAiCIAiCIAiCYGGc9VMQhp2D54q157YfoeTYCProLXMoNT5K8pswJCQDCcPOpbIa7e8b99NTmw9ReV0TZ7LcScn07"
				+ "Xtvoo+vnid5Thg0knmEYeWJDfu0x954n04VlHPuGjfOh0jT1J9GAX6+dOu8KfSd+26iFblZkvcEp5FMIwwLmw6c1f7w2i569/hF6"
				+ "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"
				+ "16OZABhQNA+8X/W7eGRjtHMJyQwgAfG+NjKOfTQLXMG7MamP0sM36Grl78qNxSDb6CBMuqIRYUGcaPth9UkcSpBRzKC4BBFlXXal"
				+ "kP59GclXMcvlXEzn/CQAG7mgyoFGEzU2UFfy2ob2fV74Z1jdKmsllo7OpT76ccW3FeUVbVqnrT7E/oiGUJwiryiCg19SyEgrnfXE"
				+ "hsewuMmIsa0QAmYj4+Pde2+6JYWepFA18QI7kP8MK++YvH7xgeW0S25kyg5LlLypnAdkimEQbH3TIH2l3V76fU9p5TgdJK/rw+P4"
				+ "PPg8tn02dsXcl0uiBNECiCNzgX3nC6kx954n3acuEj1za0cp0KHg1+4YxE9eHMu2jhKnhRMkcwhDIk33j+p/fH19wnVIXq0Hu7dF"
				+ "BU6v7h2MQ+MgRFoAGrTI06FATTK65q53V94cCDXpUJQXeJUgiNIJhGGzJXqBg1DkP2PsrjOlVRxpkI868YZmfTxVXOtsarDPJJ1p"
				+ "7Ko0OQHNem/c99NdNv8qZIHBYeRzCIMG3mFFdrfNx2gZ7cdpqqGFg7Mo40inMKu7m5eJyc9ib75wWX02TsWSd4TnEYyjTDs7D51W"
				+ "fvdKzu5NwX0q45clhQdzn1vIb4lAz4Ig0UyjjBi/HvHMQ7Mox3it+5dRgumpkl+EwRBEARBEARBEARBEARBEARBEARBEARBEARBE"
				+ "ARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBE"
				+ "ARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBEARBE"
				+ "ARBEARBEARBEARBEARBEARBELwIov8DXzUAE7yA47YAAAAASUVORK5CYII=";
	}

}
