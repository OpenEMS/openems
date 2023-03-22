package io.openems.edge.app.api;

import java.util.EnumMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.api.MqttApi.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractEnumOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;

/**
 * Describes a App for MQTT Api.
 *
 * <pre>
  {
    "appId":"App.Api.Mqtt",
    "alias":"MQTT-Api",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CONTROLLER_ID": "ctrlControllerApiMqtt0",
    	"USERNAME": "username",
    	"PASSWORD": "******",
    	"CLIENT_ID": "edge0",
    	"URI": "tcp://localhost:1883"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Api.Mqtt")
public class MqttApi extends AbstractEnumOpenemsApp<Property> implements OpenemsApp {

	public static enum Property implements Nameable {
		// Components
		CONTROLLER_ID, //
		// User-Values
		USERNAME, //
		PASSWORD, //
		CLIENT_ID, //
		URI;
	}

	@Activate
	public MqttApi(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.USERNAME) //
								.setLabel(TranslationUtil.getTranslation(bundle, "username")) //
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".Username.description")) //
								.isRequired(true) //
								.setMinLenght(3) //
								.setMaxLenght(18) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.PASSWORD) //
								.setLabel(TranslationUtil.getTranslation(bundle, "password")) //
								.setDescription(TranslationUtil.getTranslation(bundle,
										this.getAppId() + ".Password.description")) //
								.isRequired(true) //
								.setInputType(Type.PASSWORD) //
								.build()) //
						.add(JsonFormlyUtil.buildInput(Property.CLIENT_ID) //
								.setLabel(TranslationUtil.getTranslation(bundle, this.getAppId() + ".EdgeId.label")) //
								.setDescription(
										TranslationUtil.getTranslation(bundle, this.getAppId() + ".EdgeId.description")) //
								.setDefaultValue("edge0") //
								.isRequired(true) //
								.build())
						.add(JsonFormlyUtil.buildInput(Property.URI) //
								.setLabel("Uri") //
								.setDescription(
										TranslationUtil.getTranslation(bundle, this.getAppId() + ".Uri.description")) //
								.setDefaultValue("tcp://localhost:1883") //
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
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.API };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {

			var clientId = this.getValueOrDefault(p, Property.CLIENT_ID, "edge0");
			var uri = this.getValueOrDefault(p, Property.URI, "tcp://localhost:1883");

			var username = EnumUtils.getAsString(p, Property.USERNAME);
			var password = this.getValueOrDefault(p, Property.PASSWORD, "xxx");

			var controllerId = this.getId(t, p, Property.CONTROLLER_ID, "ctrlControllerApiMqtt0");

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(controllerId, this.getName(l), "Controller.Api.MQTT",
							JsonUtils.buildJsonObject() //
									.addProperty("clientId", clientId) //
									.addProperty("uri", uri) //
									.addProperty("username", username) //
									.onlyIf(t.isAddOrUpdate(), c -> c.addProperty("password", password)) //
									.build()));

			// remove password after use so it does not get save
			p.remove(Property.PASSWORD);

			return new AppConfiguration(components);
		};
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAASwAAAEsCAYAAAB5fY51AAABhWlDQ1BJQ0MgUHJvZmlsZQAAKM+VkT1Iw"
				+ "1AUhU9TpVIqgu0g4pChOlkQFXGUKBbBQmkrtOpg8tI/aNKQpLg4Cq4FB38Wqw4uzro6uAqC4A+Io5OToouUeF9SaBEqeOHxPs575"
				+ "3DffYDQqDDN6pkANN02U3FJzOZWxcArfAghiEGEZWYZifRiBl3r655uU93FeBb+V/1q3mKATySeY4ZpE28Qz2zaBud94ggrySrxO"
				+ "fG4SQ0SP3Jd8fiNc9FlgWdGzExqnjhCLBY7WOlgVjI14mniqKrplC9kPVY5b3HWKjXW6pO/MJTXV9JcpzWCOJaQQBIiFNRQRgU2Y"
				+ "rTrpFhI0bnUxT/s+pPkUshVBiPHAqrQILt+8D/4PVurMDXpJYUkoPfFcT5GgcAu0Kw7zvex4zRPAP8zcKW3/dUGMPtJer2tRY+Ag"
				+ "W3g4rqtKXvA5Q4w9GTIpuxKflpCoQC8n9E35YDwLRBc8+bWOsfpA5ChWS3fAAeHwFiRste7vLuvc25/3nHnB+kHJSZyiKHoIYQAA"
				+ "AAJcEhZcwAADsQAAA7EAZUrDhsAADTqSURBVHhe7Z0JnCVVdf/ve697dgZUBNwFFRVcUIMIJrLM9LhgTFT+aqLEYEziAgwz0z2CC"
				+ "oLszAaKQSEGo+ZPTFzyV/8uM42IyKC4x50AQYNrDLLP1t0v51dVp/u82/fW9vZb5/v5nK5zz93q1j11um69eq+MoiiKoiiKoiiKo"
				+ "iiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKo"
				+ "iiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKo"
				+ "iiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoihdppZsQ6Xo+JokWXVkmXb0cDn65LpZuOQk0k6gIb/bfPNjN5m7fh7nzaebxwptA"
				+ "LTjalvmA1cZkKVjC9L0srj6Az6d4b6Dop0DOej4JjNrotNgJ3C1J/Py6thKpM2Vz3Ce3R5g3dWOrQM7T5aRpLUzp6+aOJqSm0l/d"
				+ "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"
				+ "ocVax5iGiMUfGpvoZR8DdmnKQBNUAD6jygV/zzxi6kc3k7zlMgW8xvq5kzz4N3/YL56VWj3qXo1h2nw/AZFvw9qN+nm2PI4ZJrDo"
				+ "O4wOZR/rGMTTzHRq7KaCEqJsYnfnXoPLfE+bmp1vJhU5JldlH6vmZ4631y7pcxrtJR8aMAaMnxjk8EmT+Cx4TrsEC49T5tl+i6L3"
				+ "MfuMDbxYuqFAldNXkVZND9tZmbGzeSmWxNDr+j++AePIMfbqxOmH/jGJgNFu0GjqFPk2adeYfdZdB9k+Vg/6i9HzbKHJ697l/epm"
				+ "j8wzdpac+NVk+ZBPIPqBG0Aex989k4xfxzz8dmzsNsG3RqHDfcXFL06eP1g0Mbmcnq22Y6dpmPL5K0DbL0IXE8Cm7udFWv2NY2Rc"
				+ "6jIK2hZeK554HdXmu1XZ3+CGD7+Y9Z5etWPoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKo"
				+ "iiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKo"
				+ "iiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoiiKoigBYcz/ApOHQskEW"
				+ "P3UAAAAAElFTkSuQmCC";
	}

}
