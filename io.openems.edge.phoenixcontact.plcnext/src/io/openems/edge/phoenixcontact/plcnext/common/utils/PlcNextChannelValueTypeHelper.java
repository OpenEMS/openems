package io.openems.edge.phoenixcontact.plcnext.common.utils;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.channel.ChannelCategory;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumDoc;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataWriteValueType;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextChannelToGdsDataMapper;
import io.openems.edge.phoenixcontact.plcnext.common.mapper.PlcNextGdsDataMappingException;

public final class PlcNextChannelValueTypeHelper {

	private static final Logger log = LoggerFactory.getLogger(PlcNextChannelValueTypeHelper.class);

	private PlcNextChannelValueTypeHelper() {
	}

	public static Object getChannelValue(JsonElement jsonElement, Doc openEmsChannelDoc) {
		Object mappedValue = null;

		if (Objects.isNull(jsonElement)) {
			log.warn("JSON element is NULL! Skipping.");
		} else if (isOpenEmsEnumType(openEmsChannelDoc)) {
			mappedValue = mapToEnum(jsonElement, openEmsChannelDoc);
		} else if (isOpenEmsTypeFloat(openEmsChannelDoc)) {
			mappedValue = jsonElement.getAsFloat();
		} else if (isOpenEmsTypeDouble(openEmsChannelDoc)) {
			mappedValue = jsonElement.getAsDouble();
		} else if (isOpenEmsTypeShort(openEmsChannelDoc)) {
			mappedValue = jsonElement.getAsShort();
		} else if (isOpenEmsTypeInteger(openEmsChannelDoc)) {
			mappedValue = jsonElement.getAsInt();
		} else if (isOpenEmsTypeLong(openEmsChannelDoc)) {
			mappedValue = jsonElement.getAsLong();
		} else if (isOpenEmsTypeBoolean(openEmsChannelDoc)) {
			mappedValue = jsonElement.getAsBoolean();
		} else if (isOpenEmsTypeString(openEmsChannelDoc)) {
			mappedValue = jsonElement.getAsString();
		} else {
			throw new PlcNextGdsDataMappingException(
					"Value by type '" + openEmsChannelDoc.getType() + "' is not supported.");
		}
		return mappedValue;
	}

	private static Object mapToEnum(JsonElement jsonElement, Doc openEmsChannelDoc) {
		Object mappedValue = null;
		String sourceValue = jsonElement.getAsString();
		
		try {
			mappedValue = ((EnumDoc)openEmsChannelDoc).getOptionFromString(sourceValue);
		} catch (OpenemsNamedException e) {
			log.warn("Cannot map '{}' to ENUM {}! Trying using value.", sourceValue, ((EnumDoc)openEmsChannelDoc).getOptions());
		}
		if (Objects.isNull(mappedValue)) {
			try {
				int sourceValueInt = jsonElement.getAsInt();
				
				mappedValue = ((EnumDoc)openEmsChannelDoc).getOption(sourceValueInt);
			} catch (Exception e) {
				log.error("Cannot read ENUM value, because it's not an INT!");
				mappedValue = GridMode.UNDEFINED;
			}
		}
		return mappedValue;
	}

	public static JsonElement buildVariableToWrite(String variablePath, Object variableValue, Doc openEmsChannelDoc) {
		JsonObject mappedValue = null;

		if (Objects.isNull(variableValue)) {
			log.debug("Channel value is NULL! Skipping processing of variable '{}'.", variablePath);
		} else {
			mappedValue = new JsonObject();
			mappedValue.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_PATH, variablePath);
			mappedValue.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_VALUE_TYPE,
					PlcNextGdsDataWriteValueType.VARIABLE.getIdentifier());

			if (isOpenEmsEnumType(openEmsChannelDoc)) {
				mappedValue.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_VALUE, ((OptionsEnum)variableValue).getName());
			} else 	if (isOpenEmsTypeFloat(openEmsChannelDoc)) {
				mappedValue.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_VALUE, (Float) variableValue);
			} else if (isOpenEmsTypeDouble(openEmsChannelDoc)) {
				mappedValue.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_VALUE, (Double) variableValue);
			} else if (isOpenEmsTypeShort(openEmsChannelDoc)) {
				mappedValue.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_VALUE, (Short) variableValue);
			} else if (isOpenEmsTypeInteger(openEmsChannelDoc)) {
				mappedValue.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_VALUE, (Integer) variableValue);
			} else if (isOpenEmsTypeLong(openEmsChannelDoc)) {
				mappedValue.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_VALUE, (Long) variableValue);
			} else if (isOpenEmsTypeBoolean(openEmsChannelDoc)) {
				mappedValue.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_VALUE, (Boolean) variableValue);
			} else if (isOpenEmsTypeString(openEmsChannelDoc)) {
				mappedValue.addProperty(PlcNextChannelToGdsDataMapper.PLC_NEXT_VARIABLE_VALUE, (String) variableValue);
			} else {
				throw new PlcNextGdsDataMappingException(
						"Value by type '" + openEmsChannelDoc.getType() + "' is not supported.");
			}
		}
		return mappedValue;
	}
	
	private static boolean isOpenEmsEnumType(Doc openEmsChannelDoc) {
		return ChannelCategory.ENUM == openEmsChannelDoc.getChannelCategory();
	}

	private static boolean isOpenEmsTypeLong(Doc openEmsChannelDoc) {
		return ChannelCategory.ENUM != openEmsChannelDoc.getChannelCategory() &&
				OpenemsType.LONG == openEmsChannelDoc.getType();
	}

	private static boolean isOpenEmsTypeInteger(Doc openEmsChannelDoc) {
		return ChannelCategory.ENUM != openEmsChannelDoc.getChannelCategory() &&
				OpenemsType.INTEGER == openEmsChannelDoc.getType();
	}

	private static boolean isOpenEmsTypeShort(Doc openEmsChannelDoc) {
		return ChannelCategory.ENUM != openEmsChannelDoc.getChannelCategory() &&
				OpenemsType.SHORT == openEmsChannelDoc.getType();
	}

	private static boolean isOpenEmsTypeDouble(Doc openEmsChannelDoc) {
		return ChannelCategory.ENUM != openEmsChannelDoc.getChannelCategory() &&
				OpenemsType.DOUBLE == openEmsChannelDoc.getType();
	}

	private static boolean isOpenEmsTypeFloat(Doc openEmsChannelDoc) {
		return ChannelCategory.ENUM != openEmsChannelDoc.getChannelCategory() &&
				OpenemsType.FLOAT == openEmsChannelDoc.getType();
	}

	private static boolean isOpenEmsTypeBoolean(Doc openEmsChannelDoc) {
		return ChannelCategory.ENUM != openEmsChannelDoc.getChannelCategory() &&
				OpenemsType.BOOLEAN == openEmsChannelDoc.getType();
	}

	private static boolean isOpenEmsTypeString(Doc openEmsChannelDoc) {
		return ChannelCategory.ENUM != openEmsChannelDoc.getChannelCategory() &&
				OpenemsType.STRING == openEmsChannelDoc.getType();
	}
}
