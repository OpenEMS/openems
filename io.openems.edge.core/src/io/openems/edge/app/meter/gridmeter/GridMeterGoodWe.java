package io.openems.edge.app.meter.gridmeter;

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
import io.openems.edge.app.integratedsystem.FeneconHomeComponents;
import io.openems.edge.app.integratedsystem.GoodWeGridMeterCategory;
import io.openems.edge.app.integratedsystem.IntegratedSystemProps;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.AppManagerUtilSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ComponentUtilSupplier;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;

/**
 * Describes an app for a GoodWe grid meter.
 *
 * <pre>
 {
 "appId":"App.GridMeter.GoodWe",
 "alias":"GoodWe Netzzähler",
 "instanceId": UUID,
 "image": base64,
 "properties":{
 "METER_ID": "meter0",
 "MODBUS_ID": "modbus1",
 "GRID_METER_CATEGORY": "COMMERCIAL_METER"
 "CT_RATIO_FIRST": 200
 },
 "appDescriptor": {
 "websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
 }
 }
 * </pre>
 */
@Component(name = "App.GridMeter.GoodWe")
public class GridMeterGoodWe
		extends AbstractOpenemsAppWithProps<GridMeterGoodWe, GridMeterGoodWe.Property, BundleParameter>
		implements OpenemsApp, ComponentUtilSupplier, AppManagerUtilSupplier {

	public enum Property implements Type<Property, GridMeterGoodWe, BundleParameter> {
		METER_ID(AppDef.componentId("meter0")), //
		GRID_METER_CATEGORY(IntegratedSystemProps.gridMeterType()), //
		CT_RATIO_FIRST(IntegratedSystemProps.ctRatioFirst(GRID_METER_CATEGORY)), //
		;

		private final AppDef<? super GridMeterGoodWe, ? super Property, ? super BundleParameter> def;

		Property(AppDef<? super GridMeterGoodWe, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, GridMeterGoodWe, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super GridMeterGoodWe, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<GridMeterGoodWe>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public GridMeterGoodWe(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil, //
			@Reference final AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
		this.appManagerUtil = appManagerUtil;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {

			final var modbusExternal = "modbus0";

			final var meterId = this.getId(t, p, Property.METER_ID);
			final var gridMeterCategory = this.getEnum(p, GoodWeGridMeterCategory.class, Property.GRID_METER_CATEGORY);
			final var ctRatioFirst = this.getInt(p, Property.CT_RATIO_FIRST);

			final var bundle = AbstractOpenemsApp.getTranslationBundle(l);

			final var gridMeter = Lists.newArrayList(
					FeneconHomeComponents.gridMeter(bundle, meterId, modbusExternal, gridMeterCategory, ctRatioFirst));

			return AppConfiguration.create() //
					.addTask(Tasks.component(gridMeter)) //
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
		return OpenemsAppCardinality.SINGLE_IN_CATEGORY;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.GRID_METER };
	}

	@Override
	protected GridMeterGoodWe getApp() {
		return this;
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
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.ADMIN) //
				.setCanInstall(Role.ADMIN) //
				.setCanDelete(Role.ADMIN) //
				.build();
	}

}
