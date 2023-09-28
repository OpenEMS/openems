package io.openems.edge.app.evcs;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommunicationProps.modbusUnitId;

import java.util.Map;
import java.util.OptionalInt;
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
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.evcs.WebastoUniteEvcs.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppStatus;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;

/**
 * Describes a Webasto Unite evcs App.
 *
 * <pre>
  {
    "appId":"App.Evcs.Webasto.Unite",
    "alias":"Webasto Unite Ladestation",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "EVCS_ID": "evcs0",
      "CTRL_EVCS_ID": "ctrlEvcs0",
      "MODBUS_ID": "modbus0",
      "IP":"192.168.25.11",
      "MODBUS_UNIT_ID": 255
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Evcs.Webasto.Unite")
public class WebastoUniteEvcs extends AbstractOpenemsAppWithProps<WebastoUniteEvcs, Property, Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, WebastoUniteEvcs, Parameter.BundleParameter>, Nameable {
		// Component-IDs
		EVCS_ID(AppDef.componentId("evcs0")), //
		CTRL_EVCS_ID(AppDef.componentId("ctrlEvcs0")), //
		MODBUS_ID(AppDef.componentId("modbus0")), //
		// Properties
		ALIAS(alias()), //
		IP(AppDef.copyOfGeneric(CommunicationProps.ip(), def -> def //
				.wrapField((app, property, l, parameter, field) -> field.isRequired(true)))), //
		MODBUS_UNIT_ID(AppDef.copyOfGeneric(modbusUnitId(), def -> def //
				.setDefaultValue(255) //
		)), //
		MAX_HARDWARE_POWER_ACCEPT_PROPERTY(AppDef.of() //
				.setAllowedToSave(false)), //
		MAX_HARDWARE_POWER(EvcsProps.clusterMaxHardwarePowerSingleCp(MAX_HARDWARE_POWER_ACCEPT_PROPERTY, EVCS_ID)), //
		UNOFFICIAL_APP_WARNING(CommonProps.installationHintOfUnofficialApp()), //
		;

		private final AppDef<? super WebastoUniteEvcs, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super WebastoUniteEvcs, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, WebastoUniteEvcs, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super WebastoUniteEvcs, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<WebastoUniteEvcs>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	@Activate
	public WebastoUniteEvcs(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
			final var controllerAlias = TranslationUtil.getTranslation(bundle, "App.Evcs.controller.alias");

			// values the user enters
			final var alias = this.getString(p, l, Property.ALIAS);
			final var ip = this.getString(p, Property.IP);
			final var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);

			// values which are being auto generated by the appmanager
			final var evcsId = this.getId(t, p, Property.EVCS_ID);
			final var ctrlEvcsId = this.getId(t, p, Property.CTRL_EVCS_ID);
			final var modbusId = this.getId(t, p, Property.MODBUS_ID);

			var maxHardwarePowerPerPhase = OptionalInt.empty();
			if (p.containsKey(Property.MAX_HARDWARE_POWER)) {
				maxHardwarePowerPerPhase = OptionalInt.of(this.getInt(p, Property.MAX_HARDWARE_POWER));
			}

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(evcsId, alias, "Evcs.Webasto.Unite", JsonUtils.buildJsonObject() //
							.addProperty("modbus.id", modbusId) //
							.addProperty("modbusUnitId", modbusUnitId) //
							.build()), //
					new EdgeConfig.Component(ctrlEvcsId, controllerAlias, "Controller.Evcs", JsonUtils.buildJsonObject() //
							.addProperty("evcs.id", evcsId) //
							.build()), //
					new EdgeConfig.Component(modbusId,
							TranslationUtil.getTranslation(bundle, "App.Evcs.Webasto.Unite.modbus.alias"),
							"Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
									.addProperty("ip", ip) //
									.onlyIf(t == ConfigurationTarget.ADD, b -> b //
											.addProperty("port", 502)) //
									.build()) //
			);

			return new AppConfiguration(//
					components, //
					Lists.newArrayList(ctrlEvcsId, "ctrlBalancing0"), //
					null, //
					EvcsCluster.dependency(t, this.componentManager, this.componentUtil, maxHardwarePowerPerPhase,
							evcsId) //
			);
		};
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.EVCS };
	}

	@Override
	protected OpenemsAppStatus getStatus() {
		return OpenemsAppStatus.BETA;
	}

	@Override
	protected WebastoUniteEvcs getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}
