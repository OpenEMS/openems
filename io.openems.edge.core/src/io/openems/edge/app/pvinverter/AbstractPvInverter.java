package io.openems.edge.app.pvinverter;

import java.util.List;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;

import com.google.common.collect.Lists;

import io.openems.common.session.Language;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.app.pvinverter.FroniusPvInverter.Property;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.core.appmanager.AbstractEnumOpenemsApp;
import io.openems.edge.core.appmanager.AbstractOpenemsApp;
import io.openems.edge.core.appmanager.ComponentUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Type;
import io.openems.edge.core.appmanager.JsonFormlyUtil.InputBuilder.Validation;
import io.openems.edge.core.appmanager.Nameable;
import io.openems.edge.core.appmanager.OpenemsAppCategory;
import io.openems.edge.core.appmanager.TranslationUtil;

public abstract class AbstractPvInverter<PROPERTY extends Enum<PROPERTY> & Nameable>
		extends AbstractEnumOpenemsApp<PROPERTY> {

	protected AbstractPvInverter(ComponentManager componentManager, ComponentContext componentContext,
			ConfigurationAdmin cm, ComponentUtil componentUtil) {
		super(componentManager, componentContext, cm, componentUtil);
	}

	@Override
	public OpenemsAppCategory[] getCategories() {
		return new OpenemsAppCategory[] { OpenemsAppCategory.PV_INVERTER };
	}

	protected final List<Component> getComponents(String factoryId, String pvInverterId, //
			String modbusId, String alias, String ip, int port) {
		return Lists.newArrayList(//
				new EdgeConfig.Component(pvInverterId, alias, factoryId, //
						JsonUtils.buildJsonObject() //
								.addProperty("modbus.id", modbusId) //
								.build()), //
				new EdgeConfig.Component(modbusId, alias, "Bridge.Modbus.Tcp", JsonUtils.buildJsonObject() //
						.addProperty("ip", ip) //
						.addProperty("port", port) //
						.build())//
		);
	}

	protected static <PROPERTY extends Enum<PROPERTY>> InputBuilder buildIp(Language language, PROPERTY property) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return JsonFormlyUtil.buildInput(property) //
				.setLabel(TranslationUtil.getTranslation(bundle, "communication.ipAddress")) //
				.setDescription(TranslationUtil.getTranslation(bundle, "App.PvInverter.ip.description")) //
				.setDefaultValue("192.168.178.85") //
				.isRequired(true) //
				.setValidation(Validation.IP);
	}

	protected static <PROPERTY extends Enum<PROPERTY>> InputBuilder buildPort(Language language, PROPERTY property) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return JsonFormlyUtil.buildInput(Property.PORT) //
				.setLabel(TranslationUtil.getTranslation(bundle, "communication.port")) //
				.setDescription(TranslationUtil.getTranslation(bundle, "App.PvInverter.port.description")) //
				.setInputType(Type.NUMBER) //
				.setDefaultValue(502) //
				.setMin(0) //
				.isRequired(true);
	}

	protected static <PROPERTY extends Enum<PROPERTY>> InputBuilder buildModbusUnitId(Language language,
			PROPERTY property) {
		final var bundle = AbstractOpenemsApp.getTranslationBundle(language);
		return JsonFormlyUtil.buildInput(Property.MODBUS_UNIT_ID) //
				.setLabel(TranslationUtil.getTranslation(bundle, "communication.modbusUnitId")) //
				.setDescription(TranslationUtil.getTranslation(bundle, "App.PvInverter.modbusUnitId.description")) //
				.setInputType(Type.NUMBER) //
				.setMin(0) //
				.isRequired(true);
	}

}
