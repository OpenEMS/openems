package io.openems.edge.app.ess;

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

import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.ComponentProps;
import io.openems.edge.app.ess.FixStateOfCharge.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.OpenemsAppPermissions;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter;
import io.openems.edge.core.appmanager.Type.Parameter.BundleParameter;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.dependency.aggregatetask.SchedulerByCentralOrderConfiguration.SchedulerComponent;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.enums.InputType;

/**
 * Describes a fix state of charge app.
 *
 * <pre>
  {
    "appId":"App.Ess.FixStateOfCharge",
    "alias":"App.Ess.FixStateOfCharge",
    "instanceId": UUID,
    "image": base64,
    "properties":{
    	"ESS_ID": "ess0",
    	"TARGET_SOC": 100,
    	"TARGET_SPECIFIED_TIME": boolean,
    	"TARGET_TIME": LocalDateTime,
    	"TARGET_TIME_BUFFER": 0,
    	"SELF_TERMINATION": boolean,
    	"TERMINATION_BUFFER": 0,
    	"CONDITIONAL_TERMINATION": boolean,
    	"IS_RUNNING": boolean
    },
    "appDescriptor": {
    	"websiteUrl": {@link AppDescriptor#getWebsiteUrl()}
    }
  }
 * </pre>
 */
@Component(name = "App.Ess.FixStateOfCharge")
public class FixStateOfCharge extends AbstractOpenemsAppWithProps<FixStateOfCharge, Property, Parameter.BundleParameter>
		implements OpenemsApp {

	public enum Property implements Type<Property, FixStateOfCharge, Parameter.BundleParameter>, Nameable {
		// Components
		CTRL_FIX_STATE_OF_CHARGE_ID(AppDef.componentId("ctrlFixStateOfCharge0")), //

		// Properties
		ALIAS(alias()), //
		ESS_ID(ComponentProps.pickManagedSymmetricEssId()), //
		TARGET_SOC(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".targetSoc.label") //
				.setDefaultValue(100)//
				.setRequired(true) //
				.setField(JsonFormlyUtil::buildRangeFromNameable, (app, property, l, parameter, field) -> {
					field.setMin(1);
					field.setMax(100);
				}))), //
		TARGET_SPECIFIED_TIME(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".targetSpecifiedTime.label") //
				.setDefaultValue(false)//
				.setField(JsonFormlyUtil::buildCheckboxFromNameable) //
				.setRequired(true))), //
		TARGET_TIME(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".targetTime.label") //
				.setField(JsonFormlyUtil::buildDateTimeFromNameable, (app, property, l, parameter, field) -> {
					field.onlyShowIf(Exp.currentModelValue(TARGET_SPECIFIED_TIME).notNull());
				}) //
				.setRequired(true))), //
		TARGET_TIME_BUFFER(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".targetTimeBuffer.label") //
				.setDefaultValue(0)//
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(InputType.NUMBER);
					field.onlyPositiveNumbers();
					field.setUnit(Unit.MINUTE, l);
				}).setRequired(true))), //
		SELF_TERMINATION(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".selfTermination.label") //
				.setDefaultValue(true)//
				.setField(JsonFormlyUtil::buildCheckboxFromNameable) //
				.setRequired(true))), //
		TERMINATION_BUFFER(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".terminationBuffer.label") //
				.setDefaultValue(0)//
				.setField(JsonFormlyUtil::buildInputFromNameable, (app, property, l, parameter, field) -> {
					field.setInputType(InputType.NUMBER);
					field.onlyPositiveNumbers();
					field.setUnit(Unit.MINUTE, l);
				}).setRequired(true))), //
		CONDITIONAL_TERMINATION(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".conditionalTermination.label") //
				.setDefaultValue(false)//
				.setField(JsonFormlyUtil::buildCheckboxFromNameable) //
				.setRequired(true))), //
		IS_RUNNING(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def //
				.setTranslatedLabelWithAppPrefix(".isRunning.label") //
				.setDefaultValue(true) //
				.setField(JsonFormlyUtil::buildCheckboxFromNameable) //
				.bidirectional(CTRL_FIX_STATE_OF_CHARGE_ID, "isRunning", //
						ComponentManagerSupplier::getComponentManager))), //
		;

		private final AppDef<? super FixStateOfCharge, ? super Property, ? super BundleParameter> def;

		private Property(AppDef<? super FixStateOfCharge, ? super Property, ? super BundleParameter> def) {
			this.def = def;
		}

		@Override
		public Property self() {
			return this;
		}

		@Override
		public AppDef<? super FixStateOfCharge, ? super Property, ? super BundleParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<FixStateOfCharge>, BundleParameter> getParamter() {
			return Parameter.functionOf(AbstractOpenemsApp::getTranslationBundle);
		}
	}

	@Activate
	public FixStateOfCharge(//
			@Reference final ComponentManager componentManager, //
			final ComponentContext componentContext, //
			@Reference final ConfigurationAdmin cm, //
			@Reference final ComponentUtil componentUtil //
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
		return new OpenemsAppCategory[] { OpenemsAppCategory.ESS };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, Map<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appPropertyConfigurationFactory() {
		return (t, p, l) -> {
			final var ctrlFixStateOfChargeId = this.getId(t, p, Property.CTRL_FIX_STATE_OF_CHARGE_ID);

			final var alias = this.getString(p, l, Property.ALIAS);
			final var essId = this.getString(p, Property.ESS_ID);
			final var targetSoc = this.getInt(p, Property.TARGET_SOC);
			final var targetSpecifiedTime = this.getBoolean(p, Property.TARGET_SPECIFIED_TIME);
			final var targetTimeBuffer = this.getInt(p, Property.TARGET_TIME_BUFFER);
			final var selfTermination = this.getBoolean(p, Property.SELF_TERMINATION);
			final var terminationBuffer = this.getInt(p, Property.TERMINATION_BUFFER);
			final var conditionalTermination = this.getBoolean(p, Property.CONDITIONAL_TERMINATION);
			final var isRunning = this.getBoolean(p, Property.IS_RUNNING);

			final var properties = JsonUtils.buildJsonObject() //
					.addProperty("enabled", true) //
					.addProperty("ess.id", essId) //
					.addProperty("targetSoc", targetSoc) //
					.addProperty("targetTimeSpecified", targetSpecifiedTime) //
					.addProperty("targetTimeBuffer", targetTimeBuffer) //
					.addProperty("terminationBuffer", terminationBuffer) //
					.addProperty("conditionalTermination", conditionalTermination) //
					.addProperty("selfTermination", selfTermination) //
					.addProperty("isRunning", isRunning) //
					.build();

			if (targetSpecifiedTime) {
				final var targetTime = this.getString(p, Property.TARGET_TIME);
				properties.addProperty("targetTime", targetTime);
			}

			final var components = Lists.newArrayList(//
					new EdgeConfig.Component(ctrlFixStateOfChargeId, alias, "Controller.Ess.FixStateOfCharge", //
							properties));

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.addTask(Tasks.schedulerByCentralOrder(new SchedulerComponent(ctrlFixStateOfChargeId,
							"Controller.Ess.FixStateOfCharge", this.getAppId()))) //
					.build();
		};
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return OpenemsAppPermissions.create() //
				.setCanSee(Role.ADMIN) //
				.setCanDelete(Role.ADMIN) //
				.build();
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

	@Override
	protected FixStateOfCharge getApp() {
		return this;
	}
}
