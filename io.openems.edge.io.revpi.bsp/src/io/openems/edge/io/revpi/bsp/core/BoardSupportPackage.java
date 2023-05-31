package io.openems.edge.io.revpi.bsp.core;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.io.api.DigitalInput;
import io.openems.edge.io.api.DigitalOutput;

public interface BoardSupportPackage extends DigitalOutput, DigitalInput, OpenemsComponent {
    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

	/**
	 * Time openems is running in min.
	 */
	UPTIME(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.MINUTE) //
		.persistencePriority(PersistencePriority.HIGH) //

	),
	/**
	 * Hardware dependent status LED of the edge, used for showing the general
	 * OpenEMS state.
	 * 
	 * @note hardware may not have such an LED. Channel is ignored in that case.
	 */
	STATUS_LED_EDGE(Doc.of(LedState.values()) //
		.persistencePriority(PersistencePriority.MEDIUM) //
	),

	/**
	 * Hardware dependent status LED, used for showing the OpenEMS backend
	 * connection state.
	 * 
	 * @note hardware may not have such an LED. Channel is ignored in that case.
	 */
	STATUS_LED_BACKEND(Doc.of(LedState.values()) //
		.persistencePriority(PersistencePriority.MEDIUM) //
	), //
	/**
	 * hardware dependent status LED (reserve).
	 * 
	 * @note hardware may not have such an LED. Channel is ignored in that case.
	 */
	STATUS_LED3(Doc.of(LedState.values()) //

	), //
	/**
	 * Digital Output or Relais output 1.
	 * 
	 * <ul>
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	DIGITAL_OUT1(new BooleanDoc() //
		.unit(Unit.ON_OFF) //
		.accessMode(AccessMode.READ_WRITE) //
		.persistencePriority(PersistencePriority.MEDIUM) //
	), //
	/**
	 * Digital Output or Relais output 1.
	 *
	 * <ul>
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	DIGITAL_IN1(new BooleanDoc() //
		.unit(Unit.ON_OFF) //
		.persistencePriority(PersistencePriority.MEDIUM) //
	), //
	/**
	 * Digital Output or Relais output 1.
	 *
	 * <ul>
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	DIGITAL_IN2(new BooleanDoc() //
		.unit(Unit.ON_OFF) //
		.persistencePriority(PersistencePriority.MEDIUM) //
	), //
	/**
	 * Digital Output or Relais output 1.
	 *
	 * <ul>
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	DIGITAL_IN3(new BooleanDoc() //
		.unit(Unit.ON_OFF) //
		.persistencePriority(PersistencePriority.MEDIUM) //
	), //
	/**
	 * Digital Output or Relais output 1.
	 *
	 * <ul>
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	DIGITAL_IN4(new BooleanDoc() //
		.unit(Unit.ON_OFF) //
		.persistencePriority(PersistencePriority.MEDIUM) //
	), //

	;

	private final Doc doc;

	ChannelId(Doc doc) {
	    this.doc = doc;
	}

	public Doc doc() {
	    return this.doc;
	}
    }

    /**
     * Increments Uptime.
     * 
     * @param systemStartTime an instant holding the time OpenEMS was started.
     */
    public default void updateUptime(Instant systemStartTime) {
	var uptime = (int) Duration.between(systemStartTime, Instant.now()).toMinutes();
	this.channel(ChannelId.UPTIME).setNextValue(uptime);
    }

    public default EnumReadChannel getStatusLedEdgeChannel() {
	return this.<EnumReadChannel>channel(ChannelId.STATUS_LED_EDGE);
    }

    public default Value<LedState> getStatusLedEdgeValue() {
	return this.getStatusLedEdgeChannel().value().asEnum();
    }

    public default void setStatusLedEdgeValue(LedState value) {
	this.getStatusLedEdgeChannel().setNextValue(value);
    }

    public default EnumReadChannel getStatusLedBackendChannel() {
	return this.<EnumReadChannel>channel(ChannelId.STATUS_LED_BACKEND);
    }

    public default Value<LedState> getStatusLedBackendValue() {
	return this.getStatusLedBackendChannel().value().asEnum();
    }

    public default void setStatusLedBackendValue(LedState value) {
	this.getStatusLedBackendChannel().setNextValue(value);
    }

    public default BooleanWriteChannel getDigitalOut1WriteChannel() {
	return this.<BooleanWriteChannel>channel(ChannelId.DIGITAL_OUT1);
    }

    public default Optional<Boolean> getDigitalOut1WriteValue() {
	return this.getDigitalOut1WriteChannel().value().asOptional();
    }

    public default Optional<Boolean> getDigitalOut1WriteValueAndReset() {
	return this.getDigitalOut1WriteChannel().getNextWriteValueAndReset();
    }

    public default void setDigitalOut1Value(Boolean value) {
	this.<BooleanWriteChannel>channel(ChannelId.DIGITAL_OUT1).setNextValue(value);
    }

    public default BooleanReadChannel getDigitalIn1Channel() {
	return this.<BooleanReadChannel>channel(ChannelId.DIGITAL_IN1);
    }

    public default BooleanReadChannel getDigitalIn2Channel() {
	return this.<BooleanReadChannel>channel(ChannelId.DIGITAL_IN2);
    }

    public default BooleanReadChannel getDigitalIn3Channel() {
	return this.<BooleanReadChannel>channel(ChannelId.DIGITAL_IN3);
    }

    public default BooleanReadChannel getDigitalIn4Channel() {
	return this.<BooleanReadChannel>channel(ChannelId.DIGITAL_IN4);
    }
}
