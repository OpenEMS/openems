package io.openems.edge.app.heat;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.google.common.collect.Lists;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ComponentUtil.PreferredRelay;
import io.openems.edge.core.appmanager.ComponentUtil.RelayContactInfo;
import io.openems.edge.core.appmanager.ComponentUtil.RelayInfo;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.expression.BooleanExpression;
import io.openems.edge.core.appmanager.formly.expression.StringExpression;
import io.openems.edge.core.appmanager.formly.expression.Variable;

public final class HeatProps {

	public static record RelayContactInformation(//
			String[] preferredRelays, //
			List<RelayInfo> allRelays, //
			List<PreferredRelay> defaultRelays //
	) {

	}

	/**
	 * Provider interface for a {@link RelayContactInformation}.
	 */
	public static interface RelayContactInformationProvider {

		/**
		 * Provides a {@link RelayContactInformation}.
		 * 
		 * @return the {@link RelayContactInformation}
		 */
		RelayContactInformation relayContactInformation();

	}

	/**
	 * Utility method to create a {@link RelayContactInformation}.
	 * 
	 * @param util            the {@link ComponentUtil}
	 * @param cnt             the number of relays
	 * @param first           the first {@link PreferredRelay}
	 * @param preferredRelays the other {@link PreferredRelay PreferredRelays}
	 * @return the created {@link RelayContactInformation}
	 * @see RelayContactInformationProvider
	 */
	public static RelayContactInformation createPhaseInformation(//
			final ComponentUtil util, //
			final int cnt, //
			final PreferredRelay first, //
			final PreferredRelay... preferredRelays //
	) {
		final var preferredRelayList = Lists.newArrayList(preferredRelays);
		preferredRelayList.add(first);
		return new RelayContactInformation(//
				util.getPreferredRelays(cnt, first, preferredRelays), //
				util.getAllRelayInfos(), //
				preferredRelayList //
		);
	}

	/**
	 * Creates a {@link AppDef} for selecting a relay contact. Sets the default
	 * value from the {@link RelayContactInformation} and also disables options
	 * which are already used by other components.
	 * 
	 * @param <P>             the type of the parameter
	 * @param contactPosition the number of the contacts to select e. g. a HeatPump
	 *                        app needs 2 relay contacts to be configured pass 1 for
	 *                        the first and 2 for the second one.
	 * @param allContacts     the other names of contacts
	 * @return the {@link AppDef}
	 */
	public static <P extends BundleProvider & RelayContactInformationProvider> //
	AppDef<OpenemsApp, Nameable, P> relayContactDef(int contactPosition, Nameable... allContacts) {
		return AppDef.copyOfGeneric(defaultDef(), def -> {
			def.setDefaultValue((app, property, l, parameter) -> {
				final var preferredRelay = parameter.relayContactInformation().preferredRelays[contactPosition - 1];
				return preferredRelay == null ? JsonNull.INSTANCE : new JsonPrimitive(preferredRelay);
			});
			def.setField(JsonFormlyUtil::buildSelectGroupFromNameable, (app, property, l, parameter, field) -> {
				final var information = parameter.relayContactInformation();
				final var defaultString = " ("
						+ TranslationUtil.getTranslation(parameter.bundle(), "App.Heat.defaultRelayContact") + ")";

				final Function<RelayContactInfo, BooleanExpression> disabledExpressionFunction = channel -> {
					if (channel.usingComponents().isEmpty()) {
						return null;
					}

					var exp = Exp.initialModelValue(property).notEqual(Exp.staticValue(channel.channel()));
					for (final var nameable : allContacts) {
						if (nameable.name().equals(property.name())) {
							continue;
						}
						exp = exp.and(Exp.initialModelValue(nameable).notEqual(Exp.staticValue(channel.channel())));
					}

					return exp;
				};

				final BiFunction<RelayInfo, RelayContactInfo, StringExpression> titleExpressionFunction = (relayInfo,
						channelInfo) -> {
					final var isDefault = information.defaultRelays().stream()
							.filter(pr -> pr.numberOfRelays() == relayInfo.channels().size()) //
							.findFirst() //
							.map(t -> relayInfo.channels().get(t.preferredRelays()[contactPosition - 1] - 1)
									.equals(channelInfo))
							.orElse(false);
					final var channelDisplayName = channelInfo.getDisplayName() + (isDefault ? defaultString : "");
					if (channelInfo.usingComponents().isEmpty()) {
						return StringExpression.of(channelDisplayName);
					}

					final var componentsString = channelInfo.usingComponents().stream() //
							.map(c -> c.alias().isBlank() ? c.id() : c.alias()) //
							.map(t -> "\\'" + t + "\\'") //
							.collect(joining(", "));

					var exp = Exp.initialModelValue(property).equal(Exp.staticValue(channelInfo.channel()));
					for (final var nameable : allContacts) {
						if (nameable.name().equals(property.name())) {
							continue;
						}
						exp = exp.or(Exp.initialModelValue(nameable).equal(Exp.staticValue(channelInfo.channel())));
					}

					return Exp.ifElse(exp, //
							StringExpression.of(channelDisplayName), //
							StringExpression.of(channelDisplayName //
									+ " - " + TranslationUtil.getTranslation(parameter.bundle(),
											"App.Heat.relayContactAlreadyUsed", componentsString)) //
					);
				};
				information.allRelays().forEach(relayInfo -> {
					field.addOption(buildOptionGroup(relayInfo.id(), relayInfo.getDisplayName()) //
							.addOptions(relayInfo.channels(), (channelInfo) -> buildOption(channelInfo.channel()) //
									.setTitleExpression(titleExpressionFunction.apply(relayInfo, channelInfo))
									.onlyIf(!channelInfo.usingComponents().isEmpty(),
											b -> b.setDisabledExpression(disabledExpressionFunction.apply(channelInfo))) //
									.build())
							.build());
				});
			});
		});
	}

	/**
	 * Creates a {@link AppDef} for a group of picking contacts from relays. Used to
	 * not pick contacts twice.
	 * 
	 * @param <APP>     the type of the app
	 * @param <PROP>    the type of the property
	 * @param <P>       the type of parameter
	 * @param phaseDefs the {@link AppDef AppDefs} of the relay contacts
	 * @return the {@link AppDef}
	 */
	@SafeVarargs
	public static <APP extends OpenemsApp, //
			PROP extends Nameable & Type<PROP, APP, P>, //
			P extends BundleProvider> AppDef<APP, PROP, P> phaseGroup(//
					PROP... phaseDefs //
	) {
		return AppDef.copyOfGeneric(defaultDef(), def -> {
			def.setField(JsonFormlyUtil::buildFieldGroupFromNameable, (app, property, l, parameter, field) -> {

				for (var phaseDef : phaseDefs) {
					final var array = Exp.array(Arrays.stream(phaseDefs).filter(t -> t != phaseDef)
							.map(Exp::currentModelValue).toArray(Variable[]::new));

					final var expression = array.every(t -> Exp.currentModelValue(phaseDef).notEqual(t));
					final var errorMessage = TranslationUtil.getTranslation(parameter.bundle(),
							"App.Heat.duplicatedRelayContactSelected");
					field.setCustomValidation(phaseDef.name() + "_VALIDATION", expression,
							StringExpression.of(errorMessage), phaseDefs[phaseDefs.length - 1]);
				}
				field.setFieldGroup(Arrays.stream(phaseDefs) //
						.map(type -> type.def().getField().get(app, type, l, parameter).build()) //
						.collect(toJsonArray())) //
						.hideKey();
			});
		});
	}

	private HeatProps() {
	}

}