package io.openems.edge.core.appmanager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.common.utils.StringUtils;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.user.User;
import io.openems.edge.core.appmanager.Type.GetParameterValues;
import io.openems.edge.core.appmanager.dependency.Dependency;
import io.openems.edge.core.appmanager.flag.Flag;
import io.openems.edge.core.appmanager.flag.Flags;

public abstract class AbstractOpenemsAppWithProps<//
		APP extends AbstractOpenemsAppWithProps<APP, PROPERTY, PARAMETER>, //
		PROPERTY extends Type<PROPERTY, APP, PARAMETER> & Nameable, //
		PARAMETER  //
> extends AbstractOpenemsApp<PROPERTY> implements OpenemsApp {

	protected AbstractOpenemsAppWithProps(ComponentManager componentManager, ComponentContext componentContext,
			ConfigurationAdmin cm, ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	protected String getId(ConfigurationTarget target, Map<PROPERTY, JsonElement> map, PROPERTY property)
			throws OpenemsException {
		final var parameter = this.singletonParameter(Language.DEFAULT);
		var componentId = Optional.ofNullable(property.def().getDefaultValue())
				.map(t -> t.get(this.getApp(), property, Language.DEFAULT, parameter.get()).getAsString())
				.orElseThrow(() -> new OpenemsException(
						"No default value set for Property '" + property + "' in app '" + this.getAppId() + "'"));
		return super.getId(target, map, property, componentId);
	}

	protected JsonElement getValueOrDefault(//
			final Map<PROPERTY, JsonElement> map, //
			final Language l, //
			final PROPERTY property, //
			final Function<PROPERTY, AppDef<? super APP, ? super PROPERTY, ? super PARAMETER>> mapper)
			throws OpenemsNamedException {
		if (map.containsKey(property)) {
			return map.get(property);
		}
		final var parameter = this.singletonParameter(l);
		final var def = mapper.apply(property);
		if (def.getDefaultValue() == null) {
			throw OpenemsError.JSON_HAS_NO_MEMBER.exception(property,
					StringUtils.toShortString(map.toString(), 100).replace("%", "%%"));
		}
		return def.getDefaultValue().get(this.getApp(), property, l, parameter.get());
	}

	protected String getString(//
			final Map<PROPERTY, JsonElement> map, //
			final Language l, //
			final PROPERTY property, //
			final Function<PROPERTY, AppDef<? super APP, ? super PROPERTY, ? super PARAMETER>> mapper //
	) throws OpenemsNamedException {
		return JsonUtils.getAsString(this.getValueOrDefault(map, l, property, mapper));
	}

	protected String getString(//
			final Map<PROPERTY, JsonElement> map, //
			final Language l, //
			final PROPERTY property //
	) throws OpenemsNamedException {
		return this.getString(map, l, property, PROPERTY::def);
	}

	protected String getString(//
			final Map<PROPERTY, JsonElement> map, //
			final PROPERTY property //
	) throws OpenemsNamedException {
		return this.getString(map, Language.DEFAULT, property);
	}

	protected JsonArray getJsonArray(//
			final Map<PROPERTY, JsonElement> map, //
			final PROPERTY property //
	) throws OpenemsNamedException {
		return JsonUtils.getAsJsonArray(this.getValueOrDefault(map, Language.DEFAULT, property, PROPERTY::def));
	}

	protected int getInt(//
			final Map<PROPERTY, JsonElement> map, //
			final PROPERTY property, //
			final Function<PROPERTY, AppDef<? super APP, ? super PROPERTY, ? super PARAMETER>> mapper //
	) throws OpenemsNamedException {
		return JsonUtils.getAsInt(this.getValueOrDefault(map, Language.DEFAULT, property, mapper));
	}

	protected int getInt(//
			final Map<PROPERTY, JsonElement> map, //
			final PROPERTY property //
	) throws OpenemsNamedException {
		return this.getInt(map, property, PROPERTY::def);
	}

	protected <E extends Enum<E>> E getEnum(//
			final Map<PROPERTY, JsonElement> map, //
			final Class<E> enumType, //
			final PROPERTY property, //
			final Function<PROPERTY, AppDef<? super APP, ? super PROPERTY, ? super PARAMETER>> mapper //
	) throws OpenemsNamedException {
		return JsonUtils.getAsEnum(enumType, this.getValueOrDefault(map, Language.DEFAULT, property, mapper));
	}

	protected <E extends Enum<E>> E getEnum(//
			final Map<PROPERTY, JsonElement> map, //
			final Class<E> enumType, //
			final PROPERTY property //
	) throws OpenemsNamedException {
		return this.getEnum(map, enumType, property, PROPERTY::def);
	}

	protected boolean getBoolean(//
			final Map<PROPERTY, JsonElement> map, //
			final PROPERTY property, //
			final Function<PROPERTY, AppDef<? super APP, ? super PROPERTY, ? super PARAMETER>> mapper //
	) throws OpenemsNamedException {
		return JsonUtils.getAsBoolean(this.getValueOrDefault(map, Language.DEFAULT, property, mapper));
	}

	protected boolean getBoolean(//
			final Map<PROPERTY, JsonElement> map, //
			final PROPERTY property //
	) throws OpenemsNamedException {
		return this.getBoolean(map, property, PROPERTY::def);
	}

	@Override
	public OpenemsAppPropertyDefinition[] getProperties() {
		final var parameter = this.singletonParameter(Language.DEFAULT);
		return Arrays.stream(this.propertyValues()) //
				.map(t -> {
					return new OpenemsAppPropertyDefinition(//
							t.name(), //
							this.mapDefaultValue(t, parameter.get()), //
							t.def().isAllowedToSave(), //
							this.mapBidirectionalValue(t, parameter.get()) //
					);
				}) //
				.toArray(OpenemsAppPropertyDefinition[]::new);
	}

	@Override
	public AppAssistant getAppAssistant(User user) {
		final var language = user.getLanguage();
		final var parameter = this.singletonParameter(language);
		final var alias = this.getAlias(language, parameter.get());
		return AppAssistant.create(this.getName(language)) //
				.onlyIf(alias != null, t -> t.setAlias(alias)) //
				.fields(Arrays.stream(this.propertyValues()) //
						.filter(p -> p.def().getIsAllowedToSee() //
								.test(this.getApp(), p, language, parameter.get(), user)) //
						.filter(p -> p.def().getField() != null) //
						.map(p -> p.def().getField().get(this.getApp(), p, language, parameter.get()) //
								.readonly(!p.def().getIsAllowedToEdit() //
										.test(this.getApp(), p, language, parameter.get(), user)) //
								.build()) //
						.collect(JsonUtils.toJsonArray())) //
				.build();
	}

	private final String getAlias(Language language, PARAMETER parameter) {
		return Arrays.stream(this.propertyValues()) //
				.filter(p -> p.name().equals("ALIAS")) //
				.findFirst() //
				.flatMap(p -> {
					return Optional.ofNullable(p.def().getDefaultValue()) //
							.map(t -> t.get(this.getApp(), p, language, parameter)) // ;
							.flatMap(JsonUtils::getAsOptionalString);
				}).orElse(null);
	}

	@Override
	public AppConfiguration getAppConfiguration(//
			final ConfigurationTarget target, //
			final JsonObject config, //
			final Language language //
	) throws OpenemsNamedException {
		return super.getAppConfiguration(//
				target, //
				AbstractOpenemsApp.fillUpProperties(this, config), //
				language //
		);
	}

	@Override
	protected List<String> getValidationErrors(//
			final JsonObject jProperties, //
			final List<Dependency> dependecies //
	) {
		return super.getValidationErrors(//
				AbstractOpenemsApp.fillUpProperties(this, jProperties), //
				dependecies //
		);
	}

	private Function<Language, JsonElement> mapDefaultValue(//
			final PROPERTY property, //
			final PARAMETER parameter //
	) {
		return this.functionMapper(property, AppDef::getDefaultValue, defaultValue -> {
			return l -> {
				return defaultValue.get(this.getApp(), property, l, parameter);
			};
		});
	}

	private Function<JsonObject, JsonElement> mapBidirectionalValue(//
			final PROPERTY property, //
			final PARAMETER parameter //
	) {
		return this.functionMapper(property, AppDef::getBidirectionalValue, bidirectionalValue -> {
			return config -> {
				return bidirectionalValue.apply(this.getApp(), property, //
						Language.DEFAULT, parameter, config //
				);
			};
		});
	}

	private <M, R> R functionMapper(//
			final PROPERTY property, //
			final Function<AppDef<? super APP, ? super PROPERTY, ? super PARAMETER>, M> mapper, //
			final Function<M, R> resultMapper //
	) {
		final var firstResult = mapper.apply(property.def());
		if (firstResult == null) {
			return null;
		}

		return resultMapper.apply(firstResult);
	}

	private Singleton<PARAMETER> singletonParameter(Language l) {
		var values = this.propertyValues();
		if (values.length == 0) {
			return null;
		}
		return new Singleton<>(() -> values[0].getParamter().apply(new GetParameterValues<>(this.getApp(), l)));
	}

	public static final class Singleton<T> {

		private final Supplier<T> objectSupplier;
		private T object = null;

		public Singleton(Supplier<T> objectSupplier) {
			this.objectSupplier = objectSupplier;
		}

		/**
		 * Gets the value. If the value hasn't been created yet it gets created.
		 * 
		 * @return the value
		 */
		public final T get() {
			if (this.object == null) {
				this.object = this.objectSupplier.get();
			}
			return this.object;
		}

	}

	protected abstract APP getApp();

	@Override
	public Flag[] flags() {
		final var flags = new ArrayList<>();
		if (this.getStatus() == OpenemsAppStatus.BETA) {
			flags.add(Flags.SHOW_AFTER_KEY_REDEEM);
		}
		return flags.toArray(Flag[]::new);
	}

	protected OpenemsAppStatus getStatus() {
		return OpenemsAppStatus.STABLE;
	}

}
