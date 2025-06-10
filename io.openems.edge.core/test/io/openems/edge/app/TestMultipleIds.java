package io.openems.edge.app;

import java.util.ArrayList;
import java.util.EnumMap;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.oem.OpenemsEdgeOem;
import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.utils.EnumUtils;
import io.openems.edge.app.TestMultipleIds.Property;
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
import io.openems.edge.core.appmanager.dependency.Tasks;

/**
 * Test app for testing dependencies.
 */
@Component(name = "App.Test.TestMultipleIds")
public class TestMultipleIds extends AbstractEnumOpenemsApp<Property> implements OpenemsApp {

	public static enum Property implements Nameable {
		SET_IDS, //
		ID_1, //
		ID_2, //
		ID_3, //
		ID_4, //
		;
	}

	@Activate
	public TestMultipleIds(@Reference ComponentManager componentManager, ComponentContext componentContext,
			@Reference ConfigurationAdmin cm, @Reference ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public AppAssistant getAppAssistant(Language language) {
		return AppAssistant.create(this.getName(language)) //
				.build();
	}

	@Override
	public AppDescriptor getAppDescriptor(OpenemsEdgeOem oem) {
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
		return (t, m, l) -> {
			final var setIds = EnumUtils.getAsOptionalInt(m, Property.SET_IDS).orElse(1);

			final var components = new ArrayList<EdgeConfig.Component>();

			if (setIds >= 1) {
				components.add(new EdgeConfig.Component(this.getId(t, m, Property.ID_1, "id0"), "alias", "factoryId", //
						new JsonObject()));
			}
			if (setIds >= 2) {
				components.add(new EdgeConfig.Component(this.getId(t, m, Property.ID_2, "id0"), "alias", "factoryId", //
						new JsonObject()));
			}
			if (setIds >= 3) {
				components.add(new EdgeConfig.Component(this.getId(t, m, Property.ID_3, "id0"), "alias", "factoryId", //
						new JsonObject()));
			}
			if (setIds >= 4) {
				components.add(new EdgeConfig.Component(this.getId(t, m, Property.ID_4, "id0"), "alias", "factoryId", //
						new JsonObject()));
			}

			return AppConfiguration.create() //
					.addTask(Tasks.component(components)) //
					.build();
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
