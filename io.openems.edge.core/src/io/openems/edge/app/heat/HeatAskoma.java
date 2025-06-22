
package io.openems.edge.app.heat;

import static io.openems.edge.app.common.props.CommonProps.alias;

import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
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

/**
 * Describes a App for Heat Element.
 *
 * <pre>
  {
    "appId":"App.Heat.Askoma.ReadOnly",
    "alias":"Askoma Lesend",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"ASKOMA_ELEMENT_ID": "heat0",
    },
    "dependencies": [
    	
    ],
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Heat.Askoma.ReadOnly")
public class HeatAskoma extends AbstractOpenemsAppWithProps<HeatAskoma, HeatAskoma.Property, BundleParameter>
		implements OpenemsApp, HostSupplier {

	public static enum Property implements Type<Property, HeatAskoma, BundleParameter>, Nameable {
		// Component-IDs
		HEAT_ID(AppDef.componentId("heat0")), //
		MODBUS_ID(AppDef.componentId("modbus0")), //
		// Properties
		ALIAS(alias()), //
		IP(AppDef.copyOfGeneric(CommunicationProps.excludingIp()) //
				.setDefaultValue("192.168.2.118") //
				.setRequired(true)), //
		;

		private final AppDef<? super HeatAskoma, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super HeatAskoma, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, HeatAskoma, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super HeatAskoma, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<HeatAskoma>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	private final Host host;

	@Activate
	public HeatAskoma(//
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

			var components = Lists.newArrayList(//
					new EdgeConfig.Component(heatId, alias, "Heat.Askoma", JsonUtils.buildJsonObject() //
							.addProperty("readOnly", true) //
							.addProperty("modbus.id", modbusId) //
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
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
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
	protected HeatAskoma getApp() {
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
	
	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create()//
				.setCanDelete(Role.ADMIN)//
				.setCanSee(Role.ADMIN)//
				.build();
	}
}
