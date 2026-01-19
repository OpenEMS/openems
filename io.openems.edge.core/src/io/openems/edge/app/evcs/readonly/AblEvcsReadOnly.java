package io.openems.edge.app.evcs.readonly;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.enums.AblType.type;

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
import io.openems.edge.app.enums.AblType;
import io.openems.edge.app.evcs.readonly.AblEvcsReadOnly.Property;
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
import io.openems.edge.core.appmanager.formly.Exp;

/**
 * Describes a Abl evcs App.
 *
 * <pre>
  {
    "appId":"App.Evcs.Abl.ReadOnly",
    "alias":"ABL Ladestation",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "EVCS_ID": "evcs0",
      "EVCS_ID_2": "evcs1",
      "ALIAS": "Ladepunkt 1",
      "ALIAS_2": "Ladepunkt 2",
      "IP":"192.168.50.1",
      "TYPE" : EM_4_CONTROLLER_SINGLE,
    },
    "appDescriptor": {
      "websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Evcs.Abl.ReadOnly")
public class AblEvcsReadOnly extends AbstractOpenemsAppWithProps<AblEvcsReadOnly, Property, BundleParameter>
		implements OpenemsApp, HostSupplier {

	private final Host host;

	public enum Property implements Type<Property, AblEvcsReadOnly, BundleParameter>, Nameable {
		// Component-IDs
		EVCS_ID(AppDef.componentId("evcs0")), //
		EVCS_ID_2(AppDef.componentId("evcs1")), //
		MODBUS_ID(AppDef.componentId("modbus0")), //
		// Properties
		TYPE(AppDef.copyOfGeneric(type())), //
		ALIAS(alias()), //
		ALIAS_2(alias() //
				.wrapField(((app, property, l, parameter, field) -> //
				field.onlyShowIf(Exp.currentModelValue(TYPE).equal(Exp.staticValue(AblType.EM_4_CONTROLLER_TWIN))//
						.or(Exp.currentModelValue(TYPE)//
								.equal(Exp.staticValue(AblType.EM_4_EXTENDER_TWIN)))//
				)//
				))), //
		IP(AppDef.copyOfGeneric(CommunicationProps.excludingIp())//
				.setDefaultValue("192.168.25.11")//
				.setRequired(true)), //

		;

		private final AppDef<? super AblEvcsReadOnly, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super AblEvcsReadOnly, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, AblEvcsReadOnly, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super AblEvcsReadOnly, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<AblEvcsReadOnly>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	@Activate
	public AblEvcsReadOnly(@Reference ComponentManager componentManager, //
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
			// values the user enters
			final var ip = this.getString(p, Property.IP);
			final var prodType = this.getEnum(p, AblType.class, Property.TYPE);
			final var alias = this.getString(p, l, Property.ALIAS);
			// values which are being auto generated by the appmanager
			final var evcsId = this.getId(t, p, Property.EVCS_ID);
			final var evcsId2 = this.getId(t, p, Property.EVCS_ID_2);
			final var modbusId = this.getId(t, p, Property.MODBUS_ID);
			final var modbusAlias = TranslationUtil.getTranslation(bundle, "App.Evcs.Abl.ReadOnly.em4.modbus.alias");
			final var plug = prodType == AblType.EM_4_EXTENDER_SINGLE ? "PLUG_2" : "PLUG_1";
			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(evcsId, //
							alias, "Evcs.Abl", //
							JsonUtils.buildJsonObject() //
									.addProperty("modbus.id", modbusId) //
									.addProperty("plug", plug) //
									.build()),
					new EdgeConfig.Component(modbusId, modbusAlias, "Bridge.Modbus.Tcp", //
							JsonUtils.buildJsonObject() //
									.addProperty("ip", ip) //
									.build()) ////
			);
			if (prodType == AblType.EM_4_CONTROLLER_TWIN || prodType == AblType.EM_4_EXTENDER_TWIN) {
				final var alias2 = this.getString(p, l, Property.ALIAS_2);
				components.add(new EdgeConfig.Component(evcsId2, //
						alias2, "Evcs.Abl", //
						JsonUtils.buildJsonObject() //
								.addProperty("modbus.id", modbusId) //
								.addProperty("plug", "PLUG_2") //
								.build()));
			}

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
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.EVCS_READ_ONLY };
	}

	@Override
	protected AblEvcsReadOnly getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create()//
				.setCanDelete(Role.INSTALLER)//
				.setCanSee(Role.INSTALLER)//
				.build();
	}

	@Override
	public Host getHost() {
		return this.host;
	}
}
