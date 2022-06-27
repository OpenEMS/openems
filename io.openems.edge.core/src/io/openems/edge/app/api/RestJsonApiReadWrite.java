package io.openems.edge.app.api;

import java.util.EnumMap;
import java.util.List;

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
import io.openems.edge.app.api.RestJsonApiReadWrite.Property;
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
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;

/**
 * Describes a App for ReadWrite Rest JSON Api.
 *
 * <pre>
  {
    "appId":"App.Api.RestJson.ReadWrite",
    "alias":"Rest/JSON-Api Read-Write",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"CONTROLLER_ID": "ctrlApiRest0",
    	"API_TIMEOUT": 60
    },
    "appDescriptor": {
    	"websiteUrl": <a href=
"https://fenecon.de/fems-2-2/fems-app-rest-json-schreibzugriff-2/">https://fenecon.de/fems-2-2/fems-app-rest-json-schreibzugriff-2/</a>
    }
  }
 * </pre>
 */
@org.osgi.service.component.annotations.Component(name = "App.Api.RestJson.ReadWrite")
public class RestJsonApiReadWrite extends AbstractOpenemsApp<Property> implements OpenemsApp {

	public static enum Property {
		// Components
		CONTROLLER_ID, //
		// User-Values
		API_TIMEOUT;
	}

	@Activate
	public RestJsonApiReadWrite(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return AppAssistant.create(this.getName(language)) //
				.fields(JsonUtils.buildJsonArray() //
						.add(JsonFormlyUtil.buildInput(Property.API_TIMEOUT) //
								.setLabel(TranslationUtil.getTranslation(bundle, "App.Api.apiTimeout.label")) //
								.setDescription(
										TranslationUtil.getTranslation(bundle, "App.Api.apiTimeout.description")) //
								.setInputType(Type.NUMBER) //
								.setDefaultValue(60) //
								.setMin(30) //
								.isRequired(true) //
								.build())
						.build())
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.setWebsiteUrl("https://fenecon.de/fems-2-2/fems-app-rest-json-schreibzugriff-2/") //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategorys() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.API };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, l) -> {
			var controllerId = this.getId(t, p, Property.CONTROLLER_ID, "ctrlApiRest0");

			var apiTimeout = EnumUtils.getAsInt(p, Property.API_TIMEOUT);

			List<EdgeConfig.Component> components = Lists.newArrayList(//
					new EdgeConfig.Component(controllerId, this.getName(l), "Controller.Api.Rest.ReadWrite",
							JsonUtils.buildJsonObject() //
									.addProperty("apiTimeout", apiTimeout) //
									.build()));

			var dependencies = Lists.newArrayList(new DependencyDeclaration("READ_ONLY", //
					DependencyDeclaration.CreatePolicy.NEVER, //
					DependencyDeclaration.UpdatePolicy.ALWAYS, //
					DependencyDeclaration.DeletePolicy.NEVER, //
					DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ONLY_UNCONFIGURED_PROPERTIES, //
					DependencyDeclaration.DependencyDeletePolicy.ALLOWED, //
					DependencyDeclaration.AppDependencyConfig.create() //
							.setAppId("App.Api.RestJson.ReadOnly") //
							.setProperties(JsonUtils.buildJsonObject() //
									.addProperty(ModbusTcpApiReadOnly.Property.ACTIVE.name(),
											t == ConfigurationTarget.DELETE) //
									.build())
							.build()));

			return new AppConfiguration(components, null, null, dependencies);
		};
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public String getImage() {
		return "data:image/png;base64,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"
				+ "JKJ6biAaQe9zc7U21h5wV205vGF3PbfynuPhUoayuFcah30AC1a9i46+kO4xpIPttWE7HHcJ0ay4/4atbTcw3unhMqAldwTPYDP3"
				+ "Sdo1cX4DWd/UVs7bFL8kDANcCDIVzsS2ZfyKjkZTT3reOxHhtDgEfjM/qd5b2SoDLiVit1n8TDxtmg/L+A89z5/h7+3hUZNxHupv"
				+ "sJ7k5TZw+yoltJ9v/strW6Cr2ilaYcZIv89LH3hIMu+ltMCRyNB9oGZn2kjclrK87XLf/7WFh7enM/OCU8Rr+Igv3E7nIeJN3MeP"
				+ "+Cwa6TLA73P5aJlR7GzYsdc+C7vsbMCBfysaSltXb8fn//mcFY5pvl6WLj5ESdOII0TkbQim5j52mxMXOXrskD5PvKFLPn3ljuWH"
				+ "ULhZ8iOCrQheLf4+dTddTGtuggfzIjLA5j7A5OOZdO4hvjc+2lcValrF1f/u9Td+VladYm8FaN5SNL2MkjzOaxasTmkWuVGsdcJB"
				+ "Zqy32l8D3+d93YLlQFP8fDoUzw8+hn3OMJ6ZJHjPzGc2gYvZ2kJN+VhoRKHU7gh+tz7XaGuCZH2lTOa9ykhtvW6qLb8QSW5kc4KP"
				+ "PAH9MquoR1b5vLe5/he3hToi+y8CoX/on3e8Ceuy0GBLkvMPaGFh3/vDD+jVcAC0MhZ0eN8bKfSYzctyIizKrWJctnjAH/pIzFnJ"
				+ "HUGcB7VOI4k6bRjcsnVgfRIK1tQWe5YNoUdFb5A/I4eXTjX9Z/cIzmXb/IXQlVZ+oFFx9LXUaEFQ1185COiiJ8nfY12bL2UbvhmN"
				+ "V+myR/SxnJGc/Sw4ujtNNw3qjQCszHofdOm/s4KSFqdR2X5uhXP0soL3s3DQXyGSp4aog2czk7gQVq0fDkde8Zg3tfpBwYdS3flX"
				+ "tX3uZ438Z44K3a2xf/k7WxaueLrKZxV6Xp5MkVrz+fU84b5GS29Xy2SB7YiI29xRDiXLgcFkEZs+gfUBXV/libO/B4NHoF3Ox3GY"
				+ "RQHOKqF1Nb+dpox7xk2e5Beky/w9yPzPzaUZs3/FPcMr+Y6HcoaOXc3seP9Vz6X36LHbsbDhDT01/k3Ca9HiEuujpze17ihIjFnN"
				+ "MIpaOcjSDkuJ1UJm73Os7cstrXIJRacOYJa28/mqMVsgkWnDMywMjx4DxTew4W0fcuMI9m5zzuVpfO5+N1DZcDT7KjOpmfvvIYeW"
				+ "NX7eDwhadpghmg+h2V3AsmRhoB0Lhm4yhE7QfT9TceS3anQiqUBcBJSJ/yE5f9RV+dn6Y8XB78K7hM6lh4YzlMVjwmrglNW2MTbF"
				+ "bR9y4X0p8uxJEMTGIRiZnDVWeurPy6zneWE5nNY9aCaxoD6SDqR+9dZmTcGDwwnFeh17z6ah1+Y1NZPD1/j8CXasv4KuvH/Nu53d"
				+ "x1Ld+ayv8zVeh9XqfSjbqKrqbvrHFp10QAYo9YV8xrY5OrwDitjNNoZxDkciTMdVJysibMPG3Mc2iaJfW/mn9FK7cPZaRB+3rJzo"
				+ "AvBZ66W0N+vuZZereNXrY758GAaMvITLH2Gq4z5tIji7Vz7M9lJ3kpbs/CewgGErV3lANwMkZgzwpu7N7iQvZ1AMpI2AsnflrcuP"
				+ "03Z/cHxnxxFbe1wInAmg5Uf/D0Vu5fQdRc+ECmqY+fZBdrv5JO5V7WC92aG+Qen5DnCZ7RefPBHdPf/VPqMlsdG0raaMZrPYVUG6"
				+ "XruTA7uC5+2UQwsR2U7RlMO6Vg6kwotcCpvDhWgyEPDwreoc9sX6frL0r/1YOGSfamlFZ/RWhAqQHEL/3cJ7dj+dbrhsrRP/rIOz"
				+ "nv92oV3WBnD5hS0s6jGcUgaaQxx6bVNJTkN18Fv9DkFGjqa6OgPwbmwkynsG6qD6r/C4XO0Zd2VdOOVmKSPZ+HSCdRS+AJLH+R81"
				+ "Ge0ir/gXhU+o/VkpAMoQM6xlrNC/9W/mraVAZrHYeECipMw4wRXnNZLQ5C8bDJw5SVUih+oHPbONhozGc4GTmdCqAy4l+/JxfTnb"
				+ "62ibZbO0RHvb6eRE/AZrc/y3thQGXBn9Pqbv0T79QLnVm5aLTcH0h5zRvM5rOopNfpqGgPKlnQi2+pTez2TIscgZWHfJttZcNZYa"
				+ "m1j51P4KO8NCpUBv2EHtIwd0CPBXvh64hPZDl+nmRPoQl7kYs6jzWu/R3/9Tt7mqSqfv0ZTTRvNAM3jsOpJEqcS12DEYVVDfw0JX"
				+ "XQsm0PBp7KKcEqRsohPZX2Dh3g/p0ILPkyq4mgb73+Tujq/Qn+8pJrPaHmS4B1WxnA5FO1skjgeE0kjDcImJ8mzmrKrB+U0tgF3L"
				+ "DuRS2HHVdC9KIPib6i7eymtuujRSNFXNP74BxrSJnOGd1i1OI20jSJJnfoO1F2Xae5XQtuH8rz3DaIRO0Wfe9fzVMV7qVhYTDd9Z"
				+ "xVtxhpUK3IuzTq49PWi93H0xqWvhJk3aNRxlOMdVsboewcQj80piU7HVZZ1Q5T8zBsjiZwGSaeBzp7PgrMmUGvbF9jkX3hY+CXa9"
				+ "PKVdPNVlZ8g5h/3Oas3aC85JL8Oy+Px5A7/PiyPx5MZvMPyeDyZwTssj8eTGbzD8ng8mcE7LI/Hkxm8w/J4PJnBOyyPx5MZvMPye"
				+ "DyZwTssj8eTGbzD8ng8mcE7LI/Hkxm8w/J4PJnBOyyPx5MZvMPyeDyZwTssj8eTGbzD8ng8mcE7LI/Hkxm8w/J4PJnBOyyPx5MZv"
				+ "MPyeDyZwTssj8eTGbzD8ng8mcE7LI/Hkxm8w/J4PJnBOyyPx5MZvMPyeDyZwTssj8eTGbzD8ng8mcE7LI/Hkxm8w/J4PJnBOyyPx"
				+ "5MZvMPyeDyZwTssj8eTGbzD8ng8mcE7LI/Hkxm8w/J4PJnBOyyPx5MZvMPyeDyZwTssj8eTGbzD8ng8mcE7LI/Hkxm8w/J4PJnBO"
				+ "yyPx5MZvMPyeDyZwTssj8eTGbzD8ng8mcE7LI/Hkxm8w/J4PJnBOyyPx5MZvMPyeDyZwTssj8eTGbzD8ng8mcE7LI/HkxGI/j+cL"
				+ "nlbX6fTqwAAAABJRU5ErkJggg==";
	}

}
