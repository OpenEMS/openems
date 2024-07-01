#!/bin/bash

CHANNEL_ID="RELAY_2"
METHOD="Relay2"
JAVADOC="Relay Output 2"
#JAVADOC="Temperature in [deci degC]"

#CHANNEL_CLASS="BooleanReadChannel"
#CHANNEL_CLASS="IntegerReadChannel"
#CHANNEL_CLASS="StateChannel"
#CHANNEL_CLASS="LongReadChannel"
CHANNEL_CLASS="BooleanWriteChannel"
#CHANNEL_CLASS="IntegerWriteChannel"
#CHANNEL_CLASS="StringWriteChannel"

case "$CHANNEL_CLASS" in
	StateChannel|BooleanReadChannel|BooleanWriteChannel)
		CHANNEL_RETURN_TYPE="Value<Boolean>"
		CHANNEL_PARAM_TYPE1="boolean"
		CHANNEL_PARAM_TYPE2="boolean"
		;;
	IntegerReadChannel)
		CHANNEL_RETURN_TYPE="Value<Integer>"
		CHANNEL_PARAM_TYPE1="Integer"
		CHANNEL_PARAM_TYPE2="int"
		;;
	LongReadChannel)
		CHANNEL_RETURN_TYPE="Value<Long>"
		CHANNEL_PARAM_TYPE1="Long"
		CHANNEL_PARAM_TYPE2="long"
		;;
	IntegerWriteChannel)
		CHANNEL_RETURN_TYPE="Value<Integer>"
		CHANNEL_PARAM_TYPE1="Integer"
		CHANNEL_PARAM_TYPE2="int"
		;;
	StringWriteChannel)
		CHANNEL_RETURN_TYPE="Value<String>"
		CHANNEL_PARAM_TYPE1="String"
		CHANNEL_PARAM_TYPE2="String"
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
public default void _set${METHOD}(${CHANNEL_PARAM_TYPE1} value) {
	this.get${METHOD}Channel().setNextValue(value);
}


/**
 * Internal method to set the 'nextValue' on
 * {@link ChannelId#${CHANNEL_ID}} Channel.
 * 
 * @param value the next value
 */
public default void _set${METHOD}(${CHANNEL_PARAM_TYPE2} value) {
	this.get${METHOD}Channel().setNextValue(value);
}
EOT