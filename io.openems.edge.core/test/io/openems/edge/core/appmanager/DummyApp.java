package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.function.ThrowingTriFunction;
import io.openems.common.session.Language;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.validator.ValidatorConfig;

public class DummyApp implements OpenemsApp {

	private final String appId;
	private final OpenemsAppCategory[] categories;
	private final String name;
	private final String shortName;
	private final OpenemsAppCardinality cardinality;
	private final ValidatorConfig validatorConfig;
	private final OpenemsAppPermissions appPermissions;
	private final OpenemsAppPropertyDefinition[] properties;
	private final AppAssistant appAssistant;
	private final AppDescriptor appDescriptor;
	private final ThrowingTriFunction<ConfigurationTarget, JsonObject, Language, AppConfiguration, OpenemsNamedException> configuration;

	public static class DummyAppBuilder {

		private static int dummyAppCount = 1;

		private String appId;
		private final List<OpenemsAppCategory> categories = new ArrayList<>();
		private String name;
		private String shortName;
		private OpenemsAppCardinality cardinality;
		private ValidatorConfig validatorConfig;
		private OpenemsAppPermissions appPermissions;
		private final List<OpenemsAppPropertyDefinition> properties = new ArrayList<>();
		private AppAssistant appAssistant;
		private AppDescriptor appDescriptor;
		private ThrowingTriFunction<ConfigurationTarget, JsonObject, Language, AppConfiguration, OpenemsNamedException> configuration;

		private DummyAppBuilder() {
		}

		public DummyAppBuilder setAppId(String appId) {
			this.appId = appId;
			return this;
		}

		/**
		 * Adds Categories to the current {@link OpenemsApp}.
		 * 
		 * @param category the {@link OpenemsAppCategory OpenemsAppCategories} to add
		 * @return this
		 */
		public DummyAppBuilder addCategories(OpenemsAppCategory... category) {
			this.categories.addAll(Arrays.asList(category));
			return this;
		}

		public DummyAppBuilder setName(String name) {
			this.name = name;
			return this;
		}

		public DummyAppBuilder setShortName(String shortName) {
			this.shortName = shortName;
			return this;
		}

		public DummyAppBuilder setCardinality(OpenemsAppCardinality cardinality) {
			this.cardinality = cardinality;
			return this;
		}

		public DummyAppBuilder setValidatorConfig(ValidatorConfig validatorConfig) {
			this.validatorConfig = validatorConfig;
			return this;
		}

		public DummyAppBuilder setAppPermissions(OpenemsAppPermissions appPermissions) {
			this.appPermissions = appPermissions;
			return this;
		}

		/**
		 * Adds Properties to the current {@link OpenemsApp}.
		 * 
		 * @param properties the {@link OpenemsAppPropertyDefinition
		 *                   OpenemsAppPropertyDefinitions} to add
		 * @return this
		 */
		public DummyAppBuilder addProperties(OpenemsAppPropertyDefinition... properties) {
			this.properties.addAll(Arrays.asList(properties));
			return this;
		}

		public DummyAppBuilder setAppAssistant(AppAssistant appAssistant) {
			this.appAssistant = appAssistant;
			return this;
		}

		public DummyAppBuilder setAppDescriptor(AppDescriptor appDescriptor) {
			this.appDescriptor = appDescriptor;
			return this;
		}

		public DummyAppBuilder setConfiguration(
				ThrowingTriFunction<ConfigurationTarget, JsonObject, Language, AppConfiguration, OpenemsNamedException> configuration) {
			this.configuration = configuration;
			return this;
		}

		public DummyApp build() {
			final var name = this.name == null ? this.appId : this.name;
			return new DummyApp(//
					this.appId == null ? "Dummy.App." + (dummyAppCount++) : this.appId, //
					this.categories.toArray(OpenemsAppCategory[]::new), //
					name, //
					this.shortName == null ? name : this.shortName, //
					this.cardinality == null ? OpenemsAppCardinality.SINGLE : this.cardinality, //
					this.validatorConfig == null ? ValidatorConfig.create().build() : this.validatorConfig, //
					this.appPermissions == null ? OpenemsAppPermissions.create().build() : this.appPermissions, //
					this.properties.toArray(OpenemsAppPropertyDefinition[]::new), //
					this.appAssistant == null ? AppAssistant.create(name).build() : this.appAssistant, //
					this.appDescriptor == null ? AppDescriptor.create().build() : this.appDescriptor, //
					this.configuration == null ? (t, u, s) -> AppConfiguration.empty() : this.configuration //
			);
		}

	}

	/**
	 * Creates a builder for a {@link DummyApp}.
	 * 
	 * @return the builder
	 */
	public static DummyAppBuilder create() {
		return new DummyAppBuilder();
	}

	public DummyApp(//
			String appId, //
			OpenemsAppCategory[] categories, //
			String name, //
			String shortName, //
			OpenemsAppCardinality cardinality, //
			ValidatorConfig validatorConfig, //
			OpenemsAppPermissions appPermissions, //
			OpenemsAppPropertyDefinition[] properties, //
			AppAssistant appAssistant, //
			AppDescriptor appDescriptor, //
			ThrowingTriFunction<ConfigurationTarget, JsonObject, //
					Language, AppConfiguration, OpenemsNamedException> configuration //
	) {
		super();
		this.appId = appId;
		this.categories = categories;
		this.name = name;
		this.shortName = shortName;
		this.cardinality = cardinality;
		this.validatorConfig = validatorConfig;
		this.appPermissions = appPermissions;
		this.properties = properties;
		this.appAssistant = appAssistant;
		this.appDescriptor = appDescriptor;
		this.configuration = configuration;
	}

	@Override
	public AppAssistant getAppAssistant(User user) {
		return this.appAssistant;
	}

	@Override
	public AppConfiguration getAppConfiguration(ConfigurationTarget target, JsonObject config, Language language)
			throws OpenemsNamedException {
		return this.configuration.apply(target, config, language);
	}

	@Override
	public String getAppId() {
		return this.appId;
	}

	@Override
	public AppDescriptor getAppDescriptor() {
		return this.appDescriptor;
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return this.categories;
	}

	@Override
	public String getImage() {
		return null;
	}

	@Override
	public String getName(Language language) {
		return this.name;
	}

	@Override
	public String getShortName(Language language) {
		return this.shortName;
	}

	@Override
	public OpenemsAppCardinality getCardinality() {
		return this.cardinality;
	}

	@Override
	public OpenemsAppPropertyDefinition[] getProperties() {
		return this.properties;
	}

	@Override
	public ValidatorConfig getValidatorConfig() {
		return this.validatorConfig;
	}

	@Override
	public OpenemsAppPermissions getAppPermissions() {
		return this.appPermissions;
	}

}
