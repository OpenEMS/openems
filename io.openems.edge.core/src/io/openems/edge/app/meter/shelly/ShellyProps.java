package io.openems.edge.app.meter.shelly;

import static io.openems.edge.app.common.props.CommonProps.defaultDef;
import static io.openems.edge.core.appmanager.TranslationUtil.translate;

import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.app.enums.TranslatableEnum;
import io.openems.edge.app.meter.shelly.discovery.DiscoveryType;
import io.openems.edge.app.meter.shelly.discovery.jsonrpc.GetDiscoveredDevices;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type;
import io.openems.edge.core.appmanager.formly.Exp;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public class ShellyProps {

	/**
	 * Creates a {@link AppDef} for the discovery type field of a Shelly device.
	 *
	 * @return A new {@link AppDef} instance.
	 */
	public static AppDef<OpenemsApp, Nameable, Type.Parameter.BundleProvider> discoveryType() {
		return AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabel("communication.discoveryType.label") //
				.setDefaultValue(DiscoveryType.MDNS) //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.setOptions(OptionsFactory.of(DiscoveryType.class), l);
				}));
	}

	/**
	 * Creates a {@link AppDef} for the device selection field (searched via mdns)
	 * of a Shelly device. Only visible when mdns discovery is selected as discovery
	 * type.
	 *
	 * @param discoveryTypeProp Reference to the discovery type property, used to
	 *                          conditionally show this field only when mdns is
	 *                          selected
	 * @param apiComponentId    The component id of the api to use for fetching the
	 *                          devices, used to set the request params for the
	 *                          field
	 * @return A new {@link AppDef} instance.
	 */
	public static AppDef<OpenemsApp, Nameable, Type.Parameter.BundleProvider> mdnsDevice(Nameable discoveryTypeProp,
			String apiComponentId) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabel("App.Meter.Shelly.device.label") //
				.setField(JsonFormlyUtil::buildLazySelect, (app, property, l, parameter, field) -> {
					field.onlyShowIf(
							Exp.currentModelValue(discoveryTypeProp).equal(Exp.staticValue(DiscoveryType.MDNS)));
					field.setRequestParams(apiComponentId, GetDiscoveredDevices.METHOD);
					field.setLoadingText(translate(parameter.bundle(), "App.Meter.Shelly.device.search"));
					field.setRetryLoadingText(translate(parameter.bundle(), "App.Meter.Shelly.device.retrySearch"));
					field.setMissingOptionsText(
							translate(parameter.bundle(), "App.Meter.Shelly.device.noDevicesFound"));
				}));
	}

	/**
	 * Creates a {@link AppDef} for the hardware type selection field of a Shelly
	 * device. Only visible when ip discovery is selected as discovery type.
	 *
	 * @param discoveryTypeProp Reference to the discovery type property, used to
	 *                          conditionally show this field only when static is
	 *                          selected
	 * @param shellyTypeClass   Shelly type enum class to use for fetching the
	 *                          selectable device types and translations
	 * @param <T>               Shelly type enum class
	 * @return A new {@link AppDef} instance.
	 */
	public static <T extends Enum<T> & TranslatableEnum> AppDef<OpenemsApp, Nameable, Type.Parameter.BundleProvider> hardwareType(
			Nameable discoveryTypeProp, Class<T> shellyTypeClass) {
		return AppDef.copyOfGeneric(defaultDef(), def -> def//
				.setTranslatedLabel("App.Meter.Shelly.hardwareType") //
				.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
					field.onlyShowIf(
							Exp.currentModelValue(discoveryTypeProp).equal(Exp.staticValue(DiscoveryType.STATIC)));
					field.setOptions(OptionsFactory.of(shellyTypeClass), l);
				}));
	}
}
