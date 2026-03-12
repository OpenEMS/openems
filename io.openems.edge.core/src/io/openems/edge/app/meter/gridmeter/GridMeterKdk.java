package io.openems.edge.app.meter.gridmeter;

import static io.openems.edge.app.common.props.CommonProps.alias;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.MeterType;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.meter.MeterProps;
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
 * Describes an app for a Kdk grid meter.
 *
 * <pre>
 {
 "appId":"App.GridMeter.Kdk",
 "alias":"KDK Netzzähler",
 "instanceId": UUID,
 "image": base64,
 "properties":{
 "METER_ID": "meter0",
 "MODBUS_ID": "modbus0",
 "MODBUS_UNIT_ID": 1
 },
 "appDescriptor": {
 "websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
 }
 }
 * </pre>
 */
@Component(name = "App.GridMeter.Kdk")
public class GridMeterKdk extends AbstractOpenemsAppWithProps<GridMeterKdk, GridMeterKdk.Property, BundleParameter>
		implements OpenemsApp, ComponentUtilSupplier, AppManagerUtilSupplier {

	public enum Property implements Type<Property, GridMeterKdk, BundleParameter> {
		METER_ID(AppDef.componentId("meter0")), //
		MODBUS_ID(AppDef.componentId("modbus0")), //
		ALIAS(alias()), //
		MODBUS_UNIT_ID(AppDef.copyOfGeneric(MeterProps.modbusUnitId(), def -> def //
				.setRequired(true) //
				.setDefaultValue(1) //
				.setAutoGenerateField(false) //
		));

		private final AppDef<? super GridMeterKdk, ? super Property, ? super BundleParameter> def;

		Property(AppDef<? super GridMeterKdk, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, GridMeterKdk, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super GridMeterKdk, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<GridMeterKdk>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	private final AppManagerUtil appManagerUtil;

	@Activate
	public GridMeterKdk(//
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

			final var meterId = this.getId(t, p, Property.METER_ID);
			final var modbusId = this.getId(t, p, Property.MODBUS_ID);
			final var alias = this.getString(p, Property.ALIAS);
			final var modbusUnitId = this.getInt(p, Property.MODBUS_UNIT_ID);

			final var gridMeter = Lists
					.newArrayList(new EdgeConfig.Component(meterId, alias, "Meter.CarloGavazzi.EM300", //
							JsonUtils.buildJsonObject() //
									.addProperty("modbus.id", modbusId) //
									.addProperty("modbusUnitId", modbusUnitId) //
									.addProperty("type", MeterType.GRID) //
									.addProperty("invert", false) //
									.build()) //
			);

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
	protected GridMeterKdk getApp() {
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
