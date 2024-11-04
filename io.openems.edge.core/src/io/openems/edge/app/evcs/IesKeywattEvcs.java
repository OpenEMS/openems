package io.openems.edge.app.evcs;

import static io.openems.edge.core.appmanager.formly.enums.InputType.NUMBER;

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
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.evcs.IesKeywattEvcs.Property;
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
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Describes a IES Keywatt evcs App.
 *
 * <pre>
  {
    "appId":"App.Evcs.IesKeywatt",
    "alias":"IES Keywatt Ladestation",
    "instanceId": UUID,
    "image": base64,
    "properties":{
      "EVCS_ID": "evcs0",
      "CTRL_EVCS_ID": "ctrlEvcs0",
      "OCCP_CHARGE_POINT_IDENTIFIER":"IES 1",
      "OCCP_CONNECTOR_IDENTIFIER": "1"
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Evcs.IesKeywatt")
public class IesKeywattEvcs extends AbstractOpenemsAppWithProps<IesKeywattEvcs, Property, Parameter.BundleParameter>
		implements OpenemsApp {

	public static enum Property implements Type<Property, IesKeywattEvcs, Parameter.BundleParameter>, Nameable {
		// Component-IDs
		EVCS_ID(AppDef.componentId("evcs0")), //
		CTRL_EVCS_ID(AppDef.componentId("ctrlEvcs0")), //
		// Properties
		ALIAS(AppDef.copyOfGeneric(CommonProps.alias())), //
		OCCP_CHARGE_POINT_IDENTIFIER(AppDef.of(IesKeywattEvcs.class) //
				.setTranslatedLabelWithAppPrefix(".chargepoint.label") //
				.setTranslatedDescriptionWithAppPrefix(".chargepoint.description") //
				.setDefaultValue("IES1") //
				.setRequired(true)), //
		OCCP_CONNECTOR_IDENTIFIER(AppDef.of(IesKeywattEvcs.class) //
				.setTranslatedLabelWithAppPrefix(".connector.label") //
				.setTranslatedDescriptionWithAppPrefix(".connector.description") //
				.setDefaultValue(1) //
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> //
				field.setInputType(NUMBER) //
						.setMin(0))), //
		MAX_HARDWARE_POWER_ACCEPT_PROPERTY(AppDef.of() //
				.setAllowedToSave(false)), //
		MAX_HARDWARE_POWER(AppDef.copyOfGeneric(//
				EvcsProps.clusterMaxHardwarePowerSingleCp(MAX_HARDWARE_POWER_ACCEPT_PROPERTY, EVCS_ID))), //
		;

		private final AppDef<? super IesKeywattEvcs, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super IesKeywattEvcs, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, IesKeywattEvcs, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super IesKeywattEvcs, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<IesKeywattEvcs>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	@Activate
	public IesKeywattEvcs(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var controllerAlias = TranslationUtil.getTranslation(AbstractOpenemsApp.getTranslationBundle(l),
					"App.Evcs.controller.alias");

			// values the user enters
			final var alias = this.getString(p, l, Property.ALIAS);
			final var ocppId = this.getString(p, l, Property.OCCP_CHARGE_POINT_IDENTIFIER);
			final var connectorId = this.getInt(p, Property.OCCP_CONNECTOR_IDENTIFIER);

			var maxHardwarePowerPerPhase = OptionalInt.empty();
			if (p.containsKey(Property.MAX_HARDWARE_POWER)) {
				maxHardwarePowerPerPhase = OptionalInt.of(this.getInt(p, Property.MAX_HARDWARE_POWER));
			}

			// values which are being auto generated by the appManager
			final var evcsId = this.getId(t, p, Property.EVCS_ID);
			final var ctrlEvcsId = this.getId(t, p, Property.CTRL_EVCS_ID);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(evcsId, alias, "Evcs.Ocpp.IesKeywattSingle", JsonUtils.buildJsonObject() //
							.addProperty("ocpp.id", ocppId) //
							.addProperty("connectorId", connectorId) //
							.build()), //
					new EdgeConfig.Component(ctrlEvcsId, controllerAlias, "Controller.Evcs", JsonUtils.buildJsonObject() //
							.addProperty("evcs.id", evcsId) //
							.build())//
			);

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(
							new SchedulerComponent(ctrlEvcsId, "Controller.Evcs", this.getAppId()))) //
					.addDependencies(EvcsCluster.dependency(t, this.componentManager, this.componentUtil,
							maxHardwarePowerPerPhase, evcsId)) //
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
	protected Property[] propertyValues() {
		return Property.values();
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
	protected IesKeywattEvcs getApp() {
		return this;
	}

}
