package io.openems.edge.app.common.props;

import static io.openems.common.utils.JsonUtils.toJsonArray;
import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.formly.builder.selectgroup.Option.buildOption;
import static io.openems.edge.core.appmanager.formly.builder.selectgroup.OptionGroup.buildOptionGroup;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;

import io.openems.common.session.Language;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.hardware.IoGpio;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.ComponentUtil.PreferredRelay;
import io.openems.edge.core.appmanager.ComponentUtil.RelayContactInfo;
import io.openems.edge.core.appmanager.ComponentUtil.RelayInfo;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.TranslationUtil;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;
import io.openems.edge.core.appmanager.formly.expression.BooleanExpression;
import io.openems.edge.core.appmanager.formly.expression.StringExpression;
import io.openems.edge.core.appmanager.formly.expression.Variable;
import io.openems.edge.io.api.DigitalOutput;

public final class RelayProps {

	public static record RelayContactInformation(//
			String[] preferredRelays, //
			List<RelayInfo> allRelays, //
			List<PreferredRelay> defaultRelays //
	) {

	}

	public record RelayContactFilter(//
			Predicate<DigitalOutput> componentFilter, //
			Function<DigitalOutput, String> componentAliasMapper, //
			BiPredicate<DigitalOutput, BooleanWriteChannel> channelFilter, //
			BiFunction<DigitalOutput, BooleanWriteChannel, String> channelAliasMapper, //
			BiFunction<DigitalOutput, BooleanWriteChannel, List<String>> disabledReasons //
	) {

	}

	/**
	 * Provider interface for a {@link RelayContactInformation}.
	 */
	public interface RelayContactInformationProvider {

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
	 * @param numberOfRelays  the number of relays
	 * @param filter          the {@link RelayContactFilter}
	 * @param preferredRelays the {@link PreferredRelay PreferredRelays}
	 * @return the created {@link RelayContactInformation}
	 * @see RelayContactInformationProvider
	 */
	public static RelayContactInformation createPhaseInformation(//
			final ComponentUtil util, //
			final int numberOfRelays, //
			final List<RelayContactFilter> filter, //
			final List<PreferredRelay> preferredRelays //
	) {
		final var relayInfos = util.getAllRelayInfos(ComponentUtil.CORE_COMPONENT_IDS, //
				component -> filter.stream() //
						.map(RelayContactFilter::componentFilter) //
						.filter(Objects::nonNull) //
						.allMatch(t -> t.test(component)), //
				component -> filter.stream() //
						.map(RelayContactFilter::componentAliasMapper) //
						.filter(Objects::nonNull) //
						.map(t -> t.apply(component)) //
						.findAny().orElse(component.alias()), //
				(component, channel) -> filter.stream() //
						.map(RelayContactFilter::channelFilter) //
						.filter(Objects::nonNull) //
						.allMatch(t -> t.test(component, channel)), //
				(component, channel) -> filter.stream() //
						.map(RelayContactFilter::channelAliasMapper) //
						.filter(Objects::nonNull) //
						.map(t -> t.apply(component, channel)) //
						.findAny().orElse(channel.address().toString()), //
				(component, channel) -> filter.stream() //
						.map(RelayContactFilter::disabledReasons) //
						.filter(Objects::nonNull) //
						.map(t -> t.apply(component, channel)) //
						.flatMap(Collection::stream) //
						.toList());

		return new RelayContactInformation(//
				util.getPreferredRelays(relayInfos, numberOfRelays, preferredRelays), //
				relayInfos, //
				preferredRelays //
		);
	}

	/**
	 * Creates an empty {@link RelayContactFilter}.
	 * 
	 * @return the {@link RelayContactFilter}
	 */
	public static RelayContactFilter emptyFilter() {
		return new RelayContactFilter(t -> true, null, (t, u) -> true, null, (t, u) -> emptyList());
	}

	/**
	 * Creates a {@link RelayContactFilter} for a home.
	 * 
	 * @param l                     the current language
	 * @param isHomeInstalled       if a home is installed; can be obtained with
	 *                              {@link PropsUtil#isHomeInstalled(io.openems.edge.core.appmanager.AppManagerUtil)}
	 * @param onlyHighVoltageRelays determines which relay channels are disabled
	 * @return the {@link RelayContactFilter}
	 */
	public static RelayContactFilter feneconHomeFilter(//
			final Language l, //
			final boolean isHomeInstalled, //
			final boolean onlyHighVoltageRelays //
	) {
		if (!isHomeInstalled) {
			return emptyFilter();
		}
		final var bundle = AbstractOpenemsApp.getTranslationBundle(l);
		return new RelayContactFilter(//
				t -> true, //
				null, //
				(component, channel) -> {
					if ("io0".equals(component.id())) {
						if (List.of("Relay4", "Relay7", "Relay8").stream() //
								.anyMatch(c -> c.equals(channel.channelId().id()))) {
							return false;
						}
					}
					return true;
				}, //
				(component, channel) -> relayAliasMapper(channel), //
				(component, channel) -> {
					if (!onlyHighVoltageRelays) {
						return emptyList();
					}
					if ("io0".equals(component.id())) {
						if (List.of("Relay5", "Relay6").stream() //
								.anyMatch(c -> c.equals(channel.channelId().id()))) {
							return List.of(TranslationUtil.getTranslation(bundle, "relay.notApproved"));
						}
					}
					return emptyList();
				} //
		);
	}

	/**
	 * Creates a {@link RelayContactFilter} for {@link IoGpio} components.
	 * 
	 * @return the {@link RelayContactFilter}
	 */
	public static RelayContactFilter gpioFilter() {
		return new RelayContactFilter(t -> !t.serviceFactoryPid().equals("IO.Gpio"), null, null, null, null);
	}

	/**
	 * Creates the {@link PreferredRelay} if a Home 20/30 relay board is installed.
	 * 
	 * @param isHomeInstalled if a Home is installed
	 * @param preferredRelays the relay contacts
	 * @return the {@link PreferredRelay} configuration
	 */
	public static PreferredRelay feneconHome2030PreferredRelays(boolean isHomeInstalled, int[] preferredRelays) {
		return new PreferredRelay(relayInfo -> {
			if (!isHomeInstalled) {
				return false;
			}
			if (relayInfo.numberOfChannels() != 8) {
				return false;
			}
			return "io0".equals(relayInfo.id());
		}, preferredRelays);
	}

	// TODO remove when channels have their own alias
	private static String relayAliasMapper(BooleanWriteChannel booleanWriteChannel) {
		// TODO add translation
		for (final var iface : booleanWriteChannel.getComponent().getClass().getInterfaces()) {
			var alias = switch (iface.getCanonicalName()) {
			case "io.openems.edge.io.kmtronic.four.IoKmtronicRelay4Port" ->
				switch (booleanWriteChannel.address().getChannelId()) {
				case "Relay1" -> "Relais 1 (Pin 11/12)";
				case "Relay2" -> "Relais 2 (Pin 13/14)";
				case "Relay3" -> "Relais 3 (Pin 15/16)";
				default -> null;
				};
			case "io.openems.edge.io.kmtronic.eight.IoKmtronicRelay8Port" -> {
				if (!"io0".equals(booleanWriteChannel.getComponent().id())) {
					yield null;
				}
				yield switch (booleanWriteChannel.address().getChannelId()) {
				case "Relay1" -> "Relais 1 (Harting 10-polig, Pin 3/4, max. 230V/10A)";
				case "Relay2" -> "Relais 2 (Harting 10-polig, Pin 5/6, max. 230V/10A)";
				case "Relay3" -> "Relais 3 (Harting 10-polig, Pin 7/8, max. 230V/10A)";
				case "Relay5" -> "Relais 5 (Harting 16-polig - C, Pin 5/6, max. 24V/1A)";
				case "Relay6" -> "Relais 6 (Harting 16-polig - C, Pin 7/8, max. 24V/1A)";
				default -> null;
				};
			}
			default -> null;
			};
			if (alias != null) {
				return alias;
			}
		}
		return booleanWriteChannel.address().toString();
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
		return relayContactDef(false, contactPosition, allContacts);
	}

	/**
	 * Creates a {@link AppDef} for selecting a relay contact. Sets the default
	 * value from the {@link RelayContactInformation} and also disables options
	 * which are already used by other components.
	 * 
	 * @param <P>             the type of the parameter
	 * @param isMulti         if the selection is an array of channel
	 * @param contactPosition the number of the contacts to select e. g. a HeatPump
	 *                        app needs 2 relay contacts to be configured pass 1 for
	 *                        the first and 2 for the second one.
	 * @param allContacts     the other names of contacts
	 * @return the {@link AppDef}
	 */
	public static <P extends BundleProvider & RelayContactInformationProvider> //
	AppDef<OpenemsApp, Nameable, P> relayContactDef(boolean isMulti, int contactPosition, Nameable... allContacts) {
		return AppDef.copyOfGeneric(defaultDef(), def -> {
			def.setDefaultValue((app, property, l, parameter) -> {
				final var preferredRelay = parameter.relayContactInformation().preferredRelays[contactPosition - 1];
				final var value = preferredRelay == null ? JsonNull.INSTANCE : new JsonPrimitive(preferredRelay);
				if (isMulti) {
					return JsonUtils.buildJsonArray() //
							.add(value) //
							.build();
				}
				return value;
			});
			def.setField(JsonFormlyUtil::buildSelectGroupFromNameable, (app, property, l, parameter, field) -> {
				field.setMulti(isMulti);

				final var information = parameter.relayContactInformation();
				final var defaultString = " ("
						+ TranslationUtil.getTranslation(parameter.bundle(), "relay.defaultRelayContact") + ")";

				final Function<RelayContactInfo, BooleanExpression> disabledExpressionFunction = channel -> {
					if (channel.usingComponents().isEmpty() //
							&& channel.disabledReasons().isEmpty()) {
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
					final var isDefault = information.defaultRelays().stream() //
							.filter(t -> t.matchesRelay().test(relayInfo)) //
							.map(t -> t.preferredRelays()).findAny() //
							.map(t -> relayInfo.channels().stream()
									.filter(r -> r.position() == t[contactPosition - 1] - 1) //
									.findAny().map(channelInfo::equals).orElse(false))
							.orElse(false);

					final var channelDisplayName = channelInfo.getDisplayName() + (isDefault ? defaultString : "");
					if (channelInfo.usingComponents().isEmpty()) {
						return StringExpression.of(channelDisplayName + (channelInfo.disabledReasons().isEmpty() ? ""
								: (" - " + String.join(", ", channelInfo.disabledReasons()))));
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
											"relay.relayContactAlreadyUsed", componentsString)) //
					);
				};
				information.allRelays().forEach(relayInfo -> {
					field.addOption(buildOptionGroup(relayInfo.id(), relayInfo.getDisplayName()) //
							.addOptions(relayInfo.channels(), (channelInfo) -> buildOption(channelInfo.channel()) //
									.setTitleExpression(titleExpressionFunction.apply(relayInfo, channelInfo))
									.onlyIf(!channelInfo.usingComponents().isEmpty()
											|| !channelInfo.disabledReasons().isEmpty(),
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
							"relay.duplicatedRelayContactSelected");
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

	private RelayProps() {
	}

}