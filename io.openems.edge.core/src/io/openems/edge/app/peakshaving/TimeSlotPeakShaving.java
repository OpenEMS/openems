package io.openems.edge.app.peakshaving;

import static io.openems.edge.core.appmanager.validator.Checkables.checkAppsNotInstalled;
import static io.openems.edge.core.appmanager.validator.Checkables.checkCommercial92;
import static io.openems.edge.core.appmanager.validator.Checkables.checkIndustrial;

import java.util.Map;
import java.util.function.Function;

import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration;
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
import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.app.peakshaving.TimeSlotPeakShaving.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.AppManagerUtil;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

/**
 * Describes a time slot peak shaving app.
 *
 * <pre>
 {
 "appId":"App.PeakShaving.TimeSlotPeakShaving",
 "alias":"Hochlastzeitfenster",
 "instanceId": UUID,
 "image": base64,
 "properties":{
 "CTRL_PEAK_SHAVING_ID": "ctrlTimeSlotPeakShaving0",
 "ESS_ID": "ess0",
 "METER_ID":"meter0",
 "PEAK_SHAVING_POWER": 7000,
 "RECHARGE_POWER": 6000
 },
 "appDescriptor": {
 "websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
 }
 }
 * </pre>
 */
@Component(name = "App.PeakShaving.TimeSlotPeakShaving")
public class TimeSlotPeakShaving extends
		AbstractOpenemsAppWithProps<TimeSlotPeakShaving, Property, Parameter.BundleParameter> implements OpenemsApp {

	public enum Property implements Type<Property, TimeSlotPeakShaving, Parameter.BundleParameter> {
		// Component-IDs
		CTRL_PEAK_SHAVING_ID(AppDef.componentId("ctrlTimeSlotPeakShaving0")), //
		// Properties
		ALIAS(CommonProps.alias()), //
		ESS_ID(AppDef.copyOfGeneric(ComponentProps.pickManagedSymmetricEssId(), def -> def //
				.setRequired(true) //
				.bidirectional(CTRL_PEAK_SHAVING_ID, "ess", //
						ComponentManagerSupplier::getComponentManager))), //
		METER_ID(AppDef.copyOfGeneric(ComponentProps.pickElectricityGridMeterId(), def -> def //
				.setRequired(true) //
				.bidirectional(CTRL_PEAK_SHAVING_ID, "meter.id", //
						ComponentManagerSupplier::getComponentManager))), //
		;

		private final AppDef<? super TimeSlotPeakShaving, ? super Property, ? super BundleParameter> def;

		Property(AppDef<? super TimeSlotPeakShaving, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, TimeSlotPeakShaving, BundleParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super TimeSlotPeakShaving, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<TimeSlotPeakShaving>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}

	}

	@Activate
	public TimeSlotPeakShaving(//
			@Reference ComponentManager componentManager, //
			ComponentContext componentContext, //
			@Reference ConfigurationAdmin cm, //
			@Reference ComponentUtil componentUtil, //
			@Reference AppManagerUtil appManagerUtil //
	) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
		return AppDescriptor.create() //
				.setWebsiteUrl(oem.getAppWebsiteUrl(this.getAppId())) //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.PEAK_SHAVING };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected TimeSlotPeakShaving getApp() {
		return this;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, m, l) -> {
			final var ctrlPeakShavingId = this.getId(t, m, Property.CTRL_PEAK_SHAVING_ID);

			final var alias = this.getString(m, l, Property.ALIAS);
			final var essId = this.getString(m, Property.ESS_ID);
			final var meterId = this.getString(m, Property.METER_ID);

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlPeakShavingId, alias, "Controller.TimeslotPeakshaving",
							JsonUtils.buildJsonObject() //
									.addProperty("ess", essId) //
									.addProperty("meter.id", meterId) //
									.onlyIf(t == ConfigurationTarget.ADD, b -> {
										b //
												.addProperty("startDate", "30.12.1998") //
												.addProperty("endDate", "31.12.1998") //
												.addProperty("startTime", "7:00") //
												.addProperty("slowChargeStartTime", "12:00") //
												.addProperty("endTime", "17:00") //
												.addProperty("peakShavingPower", 10000) //
												.addProperty("rechargePower", 5000) //
												.addProperty("hysteresisSoc", 50) //
										;
									}) //
									.build()));

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(//
							new SchedulerByCentralOrderConfiguration.SchedulerComponent(ctrlPeakShavingId,
									"Controller.TimeslotPeakshaving", this.getAppId()))) //
					.build();
		};
	}

	@Override
	protected ValidatorConfig.Builder getValidateBuilder() {
		return ValidatorConfig.create() //
				.setCompatibleCheckableConfigs(checkIndustrial().or(checkCommercial92()))
				.setInstallableCheckableConfigs(checkAppsNotInstalled("App.PeakShaving.PeakShaving",
						"App.PeakShaving.PhaseAccuratePeakShaving"));
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}