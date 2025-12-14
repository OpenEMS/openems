package io.openems.edge.app;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;

import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Function;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.TestPermissions.Property;
import io.openems.edge.app.TestPermissions.TestPermissionsParameter;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractOpenemsAppWithProps;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.dependency.Tasks;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.builder.ReorderArrayBuilder;
import io.openems.edge.core.appmanager.formly.enums.DisplayType;

/**
 * Tests AppPropertyPermissions.
 */
@org.osgi.service.component.annotations.Component(name = "App.Test.TestPermissions")
public class TestPermissions extends AbstractOpenemsAppWithProps<TestPermissions, Property, TestPermissionsParameter>
		implements OpenemsApp {

	public record TestPermissionsParameter(//
			ResourceBundle bundle //
	) implements BundleProvider {

	}

	public static enum Property implements Type<Property, TestPermissions, TestPermissionsParameter> {
		ID(AppDef.componentId("id0")), //
		ADMIN_ONLY(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def//
				.setMinRole(Role.ADMIN))), //
		INSTALLER_ONLY(AppDef.copyOfGeneric(CommonProps.defaultDef(), def -> def//
				.setMinRole(Role.INSTALLER))), //
		EVERYONE(AppDef.copyOfGeneric(CommonProps.defaultDef())), //
		UPDATE_ARRAY(AppDef.copyOfGeneric(defaultDef(), def -> def //
				.setTranslatedLabel("component.id.plural") //
				.setField(JsonFormlyUtil::buildFieldGroupFromNameable, (app, property, l, parameter, field) -> {
					field.setPopupInput(property, DisplayType.STRING);

					final var arrayBuilder = new ReorderArrayBuilder(property); //
					final var fields = JsonUtils.buildJsonArray() //
							.add(arrayBuilder.build());

					field.setFieldGroup(fields.build());
				})).setDefaultValue((app, property, l, parameter) -> {
					return JsonUtils.buildJsonArray().add("val1").add("val2").build();
				})),

		;//

		private final AppDef<? super TestPermissions, ? super Property, ? super TestPermissionsParameter> def;

		private Property(AppDef<? super TestPermissions, ? super Property, ? super TestPermissionsParameter> def) {
			this.def = def;
		}

		@Override
		public Type<Property, TestPermissions, TestPermissionsParameter> self() {
			return this;
		}

		@Override
		public AppDef<? super TestPermissions, ? super Property, ? super TestPermissionsParameter> def() {
			return this.def;
		}

		@Override
		public Function<GetParameterValues<TestPermissions>, TestPermissionsParameter> getParamter() {
			return t -> {
				return new TestPermissionsParameter(//
						createResourceBundle(t.language) //
				);
			};
		}

	}

	@Activate
	public TestPermissions(//
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

			final var components = new ArrayList<EdgeConfig.Component>();
			// final var updateArray = this.getJsonArray(p, Property.UPDATE_ARRAY);
			components.add(new EdgeConfig.Component(this.getId(t, p, Property.ADMIN_ONLY, "id0"), "alias", "factoryId", //
					new JsonObject()));
			components.add(
					new EdgeConfig.Component(this.getId(t, p, Property.INSTALLER_ONLY, "id0"), "alias", "factoryId", //
							new JsonObject()));
			components.add(new EdgeConfig.Component(this.getId(t, p, Property.EVERYONE, "id0"), "alias", "factoryId", //
					new JsonObject()));
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
		return new OpenemsAppCategory[] { OpenemsAppCategory.TEST };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.SINGLE;
	}

	@Override
	protected TestPermissions getApp() {
		return this;
	}

	@Override
	protected Property[] propertyValues() {
		return Property.values();
	}

}
