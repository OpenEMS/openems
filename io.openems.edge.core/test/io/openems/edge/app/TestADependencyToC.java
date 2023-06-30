package io.openems.edge.app;

import java.util.EnumMap;

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
import io.openems.common.utils.EnumUtils;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.TestADependencyToC.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractEnumOpenemsApp;
import io.openems.edge.core.appmanager.AppAssistant;
import io.openems.edge.core.appmanager.AppConfiguration;
import io.openems.edge.core.appmanager.AppDescriptor;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ConfigurationTarget;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.OpenemsAppCardinality;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration;
import io.openems.edge.core.appmanager.dependency.DependencyDeclaration.AppDependencyConfig;

/**
 * Test app for testing dependencies.
 */
@Component(name = "App.Test.TestADependencyToC")
public class TestADependencyToC extends AbstractEnumOpenemsApp<Property> implements OpenemsApp {

	public static enum Property implements Nameable {
		CREATE_POLICY, //
		UPDATE_POLICY, //
		DELETE_POLICY, //
		DEPENDENCY_UPDATE_POLICY, //
		DEPENDENCY_DELETE_POLICY, //
		NUMBER
	}

	@Activate
	public TestADependencyToC(@Reference ComponentManager componentManager, ComponentContext context,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, context, cm, componentUtil);
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		return AppAssistant.create(this.getName(language)) //
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return AppDescriptor.create() //
				.build();
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.TEST };
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return OpenemsAppCardinality.MULTIPLE;
	}

	@Override
	protected ThrowingTriFunction<ConfigurationTarget, EnumMap<Property, JsonElement>, Language, AppConfiguration, OpenemsNamedException> appConfigurationFactory() {
		return (t, p, s) -> {

			var createPolicy = Enum.valueOf(DependencyDeclaration.CreatePolicy.class,
					EnumUtils.getAsOptionalString(p, Property.CREATE_POLICY)
							.orElse(DependencyDeclaration.CreatePolicy.IF_NOT_EXISTING.name()));

			var updatePolicy = Enum.valueOf(DependencyDeclaration.UpdatePolicy.class,
					EnumUtils.getAsOptionalString(p, Property.UPDATE_POLICY)
							.orElse(DependencyDeclaration.UpdatePolicy.ALWAYS.name()));

			var deletePolicy = Enum.valueOf(DependencyDeclaration.DeletePolicy.class,
					EnumUtils.getAsOptionalString(p, Property.DELETE_POLICY)
							.orElse(DependencyDeclaration.DeletePolicy.IF_MINE.name()));

			var dependencyUpdatePolicy = Enum.valueOf(DependencyDeclaration.DependencyUpdatePolicy.class,
					EnumUtils.getAsOptionalString(p, Property.DEPENDENCY_UPDATE_POLICY)
							.orElse(DependencyDeclaration.DependencyUpdatePolicy.ALLOW_ALL.name()));

			var dependencyDeletePolicy = Enum.valueOf(DependencyDeclaration.DependencyDeletePolicy.class,
					EnumUtils.getAsOptionalString(p, Property.DEPENDENCY_DELETE_POLICY)
							.orElse(DependencyDeclaration.DependencyDeletePolicy.ALLOWED.name()));

			var number = EnumUtils.getAsOptionalInt(p, Property.NUMBER).orElse(0);

			var dependencies = Lists.newArrayList(new DependencyDeclaration("C", //
					createPolicy, updatePolicy, deletePolicy, //
					dependencyUpdatePolicy, dependencyDeletePolicy, //
					AppDependencyConfig.create() //
							.setAppId("App.Test.TestC") //
							.setProperties(JsonUtils.buildJsonObject() //
									.addProperty(TestC.Property.NUMBER.name(), number) //
									.build())
							.build()) //
			);

			return new AppConfiguration(null, null, null, dependencies);
		};
	}

	@Override
	protected Class<Property> getPropertyClass() {
		return Property.class;
	}

	@Override
	public String getName(Language language) {
		return this.getAppId();
	}

}
