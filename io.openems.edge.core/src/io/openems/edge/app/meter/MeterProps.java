package io.openems.edge.app.meter;

import io.openems.edge.app.common.props.CommonProps;
import io.openems.edge.app.common.props.CommunicationProps;
import io.openems.edge.app.enums.MeterType;
import io.openems.edge.app.enums.OptionsFactory;
import io.openems.edge.core.appmanager.AppDef;
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
	 * @param <P>     the type of the parameters
	 * @param exclude {@link MeterType}s to exclude
	 * @return the {@link AppDef}
	 */
	public static final <P extends BundleProvider> AppDef<OpenemsApp, Nameable, P> type(MeterType... exclude) {
		final var optionsFactory = OptionsFactory.of(MeterType.class, exclude);
		return AppDef.copyOfGeneric(CommonProps.defaultDef(), //
				def -> def.setTranslatedLabel("App.Meter.mountType.label") //
						.setDefaultValue(MeterType.PRODUCTION) //
						.setField(JsonFormlyUtil::buildSelectFromNameable, (app, property, l, parameter, field) -> {
							field.setOptions(optionsFactory, l);
						}));
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
	 * @param <P> the type of the parameters
	 * @return the {@link AppDef}
	 * @see CommunicationProps#modbusUnitId()
	 */
	public static final <P extends BundleProvider> AppDef<OpenemsApp, Nameable, P> modbusUnitId() {
		return AppDef.copyOfGeneric(CommunicationProps.modbusUnitId(), def -> {
			def.setTranslatedDescription("App.Meter.modbusUnitId.description");
		});
	}

}
