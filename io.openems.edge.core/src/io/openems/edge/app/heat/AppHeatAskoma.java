package io.openems.edge.app.heat;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.host.Host;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.HostSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Describes an App for a writable ASKOMA heating element.
 *
 * <pre>
  {
    "appId":"App.Heat.Askoma",
    "alias":"ASKOMA",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"HEAT_ID": "heat0",
    	"MODBUS_ID": "modbus0",
    	"IP": "192.168.2.118",
	   "MAX_HEAT_POWER": 30000,
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Heat.Askoma")
public class AppHeatAskoma extends AbstractOpenemsAppWithProps<AppHeatAskoma, AppHeatAskoma.Property, BundleParameter>
		implements OpenemsApp, HostSupplier {

	public static enum Property implements Type<Property, AppHeatAskoma, BundleParameter>, Nameable {
		// Component-IDs
		HEAT_ID(AppDef.componentId("heat0")), //
		MODBUS_ID(AppDef.componentId("modbus0")), //
		// Properties
		ALIAS(alias()), //
		IP(AppDef.copyOfGeneric(CommunicationProps.excludingIp())//
				.setDefaultValue("192.168.2.118")//
				.setRequired(true)), //
		MAX_HEAT_POWER(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setDefaultValue(30000) //
				.setRequired(true) //
				.setTranslatedLabelWithAppPrefix(".maxHeatPower.label") //
				.setTranslatedDescriptionWithAppPrefix(".maxHeatPower.description"))
				.setField(JsonFormlyUtil::buildInputFromNameable,
						(app, property, l, parameter, field) -> field.setInputType(NUMBER)//
								.setMin(250)//
								.setMax(30000)//
								.setUnit(Unit.WATT, l)));

		private final AppDef<? super AppHeatAskoma, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super AppHeatAskoma, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, AppHeatAskoma, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super AppHeatAskoma, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AppHeatAskoma>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	private final Host host;

	@Activate
	public AppHeatAskoma(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil, //
			@Reference Host host //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.host = host;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);

			final var heatId = this.getId(t, p, Property.HEAT_ID);
			final var modbusId = this.getId(t, p, Property.MODBUS_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var ip = this.getString(p, l, Property.IP);
			final var maxHeatPower = this.getInt(p, Property.MAX_HEAT_POWER);

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(heatId, alias, "Heat.Askoma", JsonUtils.buildJsonObject() //
							.addProperty("readOnly", false) //
							.addProperty("modbus.id", modbusId) //
							.addProperty("maxHeatPower", maxHeatPower) //
							.build()), //
					new EdgeConfig.Component(modbusId,
							TranslationUtil.getTranslation(bundle, "App.Heat.Askoma.modbus.alias"), "Bridge.Modbus.Tcp",
							JsonUtils.buildJsonObject() //
									.addProperty("ip", ip) //
									.onlyIf(t == ConfigurationTarget.ADD, b -> b //
											.addProperty("port", 502)) //
									.build()) //
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.build();
		};
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanInstall(Role.ADMIN) //
				.setCanSee(Role.ADMIN) //
				.setCanDelete(Role.ADMIN) //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.HEAT };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected AppHeatAskoma getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public Host getHost() {
		return this.host;
	}
}
