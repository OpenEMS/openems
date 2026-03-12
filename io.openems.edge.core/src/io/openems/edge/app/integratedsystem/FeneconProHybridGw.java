package io.openems.edge.app.integratedsystem;

import static io.openems.edge.app.common.props.CommonProps.alias;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.common.collect.Lists;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

/**
 * Describes a FENECON Pro Hybrid GW system.
 *
 * <pre>
 	{
		 "appId":"App.FENECON.ProHybrid.GW",
		 "alias":"FENECON Pro Hybrid GW",
		 "instanceId": UUID,
		 "image": base64,
		 "properties":{
			 "SERIAL_NUMBER" : null,
			 "IP" : null,
			 "USER_KEY" : "xxx"
	 		}
 	}
 * </pre>
 */
@Component(name = "App.FENECON.ProHybrid.GW")
public class FeneconProHybridGw
		extends AbstractOpenemsAppWithProps<FeneconProHybridGw, FeneconProHybridGw.Property, Parameter.BundleParameter>
		implements OpenemsApp, AppManagerUtilSupplier {

	public enum Property implements Type<Property, FeneconProHybridGw, Parameter.BundleParameter> {
		ALIAS(alias()), //
		// DC PV Charger 1
		HAS_DC_PV1(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabel("App.FENECON.Home.hasDcPV1.label")//
				.setDefaultValue(false)//
				.setField(JsonFormlyUtil::buildCheckboxFromNameable))), //
		DC_PV1_ALIAS(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setLabel("DC-PV 1 Alias") //
				.setDefaultValue("charger0") //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(HAS_DC_PV1).notNull());
				}))), //

		// DC PV Charger 2
		HAS_DC_PV2(AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabel("App.FENECON.Home.hasDcPV2.label")//
				.setDefaultValue(false)//
				.setField(JsonFormlyUtil::buildCheckboxFromNameable))), //
		DC_PV2_ALIAS(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setLabel("DC-PV 2 Alias") //
				.setDefaultValue("charger1") //
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(HAS_DC_PV2).notNull());
				}))), //
		;

		private final AppDef<? super FeneconProHybridGw, ? super Property, ? super Parameter.BundleParameter> def;

		Property(AppDef<? super FeneconProHybridGw, ? super Property, ? super Parameter.BundleParameter> def) {
			this.def = def;
		}

		@Override
		public AppDef<? super FeneconProHybridGw, ? super Property, ? super Parameter.BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FeneconProHybridGw>, Parameter.BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

		@Override
		public Type<Property, FeneconProHybridGw, Parameter.BundleParameter> self() {
			return this;
		}
	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public FeneconProHybridGw(@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil, //
			@Reference final AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	protected FeneconProHybridGw getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsError.OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {

			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);

			final var gridMeterId = "meter0";
			final var essId = "ess0";
			final var modbusId = "modbus0";
			final var modbusUnitId = 247;

			final var hasPvs = List.of(this.getBoolean(p, Property.HAS_DC_PV1),
					this.getBoolean(p, Property.HAS_DC_PV2));

			final var pvAliases = List.of(this.getStringOrNull(p, l, Property.DC_PV1_ALIAS),
					this.getStringOrNull(p, l, Property.DC_PV2_ALIAS));

			final var components = Lists.newArrayList(//
					ProHybridGwComponents.ess(bundle, essId), //
					ProHybridGwComponents.gridMeter(bundle, gridMeterId, modbusId, modbusUnitId), //
					ProHybridGwComponents.modbus(modbusId));

			for (int i = 0; i < hasPvs.size(); i++) {
				var factoryIdIndex = i + 1;
				if (hasPvs.get(i)) {
					components.add(ProHybridGwComponents.dcPv(//
							"charger" + i, pvAliases.get(i), //
							"GoodWe.Charger-PV" + factoryIdIndex, //
							essId, //
							modbusId, //
							modbusUnitId));
				}
			}

			return AppConfiguration.create() //
					.addTask(Tasks.componentFromComponentConfig(components)) //
					.build();
		};
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	public AppManagerUtil getAppManagerUtil() {
		return this.appManagerUtil;
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.INSTALLER) //
				.setCanDelete(Role.INSTALLER) //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.INTEGRATED_SYSTEM };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}
}
