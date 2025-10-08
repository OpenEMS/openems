package io.openems.edge.app.meter;

import io.openems.common.session.Role;
import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.enums.MeterType;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.core.appmanager.AppDef;
import io.openems.edge.core.appmanager.ComponentManagerSupplier;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsApp;
import io.openems.edge.core.appmanager.Type.Parameter.BundleProvider;
import io.openems.edge.core.appmanager.formly.JsonFormlyUtil;

public final class MeterProps {

	private MeterProps() {
	}

	/**
	 * Creates a {@link AppDef} for a {@link MeterType}.
	 * 
	 * @param exclude {@link MeterType}s to exclude
	 * @return the {@link AppDef}
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> type(MeterType... exclude) {
		final var optionsFactory = OptionsFactory.of(MeterType.class, exclude);
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), //
				def -> def.setTranslatedLabel("App.Meter.mountType.label") //
						.setDefaultValue(MeterType.PRODUCTION) //
						.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
							field.setOptions(optionsFactory, l);
						}));
	}

	/**
	 * Creates a {@link AppDef} for a boolean for a invertion of a meter.
	 * 
	 * @param <APP> the type of the app
	 * @param prop  {@link Nameable} referencing the meter id
	 * @return the {@link AppDef}
	 */
	public static final <APP extends OpenemsApp & ComponentManagerSupplier> AppDef<? super APP, Nameable, BundleProvider> invert(
			final Nameable prop) {
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), //
				def -> def.setTranslatedLabel("App.Meter.invert.label") //
						.setDefaultValue(false) //
						.setField(JsonFormlyUtil::buildCheckboxFromNameable) //
						.bidirectional(prop, "invert", ComponentManagerSupplier::getComponentManager)
						.setIsAllowedToSee(AppDef.ofLeastRole(Role.INSTALLER))//
		);
	}

	/**
	 * Creates a {@link AppDef} for a ip for a meter.
	 * 
	 * @param <P> the type of the parameters
	 * @return the {@link AppDef}
	 * @see CommunicationProps#ip()
	 */
	public static final <P extends BundleProvider> AppDef<OpenemsApp, Nameable, P> ip() {
		return AppDef.copyOfGeneric(CommunicationProps.ip(), def -> {
			def.setTranslatedDescription("App.Meter.ip.description");
		});
	}

	/**
	 * Creates a {@link AppDef} for a port for a meter.
	 * 
	 * @param <P> the type of the parameters
	 * @return the {@link AppDef}
	 * @see CommunicationProps#port()
	 */
	public static final <P extends BundleProvider> AppDef<OpenemsApp, Nameable, P> port() {
		return AppDef.copyOfGeneric(CommunicationProps.port(), def -> {
			def.setTranslatedDescription("App.Meter.port.description");
		});
	}

	/**
	 * Creates a {@link AppDef} for a modbusUnitId for a meter.
	 * 
	 * @return the {@link AppDef}
	 * @see CommunicationProps#modbusUnitId()
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> modbusUnitId() {
		return AppDef.copyOfGeneric(CommunicationProps.modbusUnitId(), def -> {
			def.setTranslatedDescription("App.Meter.modbusUnitId.description");
		});
	}

	/**
	 * Creates a {@link AppDef} for a phaseRotation for a meter.
	 *
	 * @return the {@link AppDef}
	 * @see CommonProps#phaseRotation()
	 */
	public static final AppDef<OpenemsApp, Nameable, BundleProvider> phaseRotation() {
		return AppDef.copyOfGeneric(
				CommonProps.phaseRotation() //
						.setTranslatedDescription("App.Meter.phaseRotation.description"));
	}

}
