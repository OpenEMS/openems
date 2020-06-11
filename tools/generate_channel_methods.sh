#!/bin/bash

CHANNEL_ID="MAX_CELL_VOLTAGE"
METHOD="MaxCellVoltage"
JAVADOC="Maximum Cell Voltage in [mV]"
#JAVADOC="Run-Failed State"

CHANNEL_CLASS="IntegerReadChannel"
#CHANNEL_CLASS="StateChannel"
#CHANNEL_CLASS="LongReadChannel"

case "$CHANNEL_CLASS" in
	StateChannel)
		CHANNEL_RETURN_TYPE="Value<Boolean>"
		CHANNEL_PARAM_TYPE="boolean"
		;;
	IntegerReadChannel)
		CHANNEL_RETURN_TYPE="Value<Integer>"
		CHANNEL_PARAM_TYPE="Integer"
		;;
	LongReadChannel)
		CHANNEL_RETURN_TYPE="Value<Long>"
		CHANNEL_PARAM_TYPE="Long"
		;;
	*)
		echo "Unknown Class ${CHANNEL_CLASS}"
		exit 1
esac

cat <<EOT
/**
 * Gets the Channel for {@link ChannelId#${CHANNEL_ID}}.
 * 
 * @return the Channel
 */
public default ${CHANNEL_CLASS} get${METHOD}Channel() {
	return this.channel(ChannelId.${CHANNEL_ID});
}

/**
 * Gets the ${JAVADOC}. See
 * {@link ChannelId#${CHANNEL_ID}}.
 * 
 * @return the Channel {@link Value}
 */
public default ${CHANNEL_RETURN_TYPE} get${METHOD}() {
	return this.get${METHOD}Channel().value();
}

/**
 * Internal method to set the 'nextValue' on
 * {@link ChannelId#${CHANNEL_ID}} Channel.
 * 
 * @param value the next value
 */
public default void _set${METHOD}(${CHANNEL_PARAM_TYPE} value) {
	this.get${METHOD}Channel().setNextValue(value);
}
EOT