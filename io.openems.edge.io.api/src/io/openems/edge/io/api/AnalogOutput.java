package io.openems.edge.io.api;

import java.util.function.Consumer;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.common.utils.IntUtils;
import io.openems.common.utils.IntUtils.Round;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.FloatDoc;
import io.openems.edge.common.channel.FloatReadChannel;
import io.openems.edge.common.channel.FloatWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface AnalogOutput extends OpenemsComponent {

	/**
	 * Output will be set in Unit.PERCENT, because the power is strongly depending
	 * on the controller and its power behavior.
	 */
	public static final int SET_OUTPUT_ACCURACY = 100;

	/**
	 * Gets the information about the AnalogOutput range, to handle its output
	 * values.
	 * 
	 * @param offset    The offset of the range e.g. offset of 6000 if the target
	 *                  should be given from 6A to 24A
	 * @param precision Gets the smallest positive value that can be set. Unit is
	 *                  depending on the control Unit. Example:
	 *                  <ul>
	 *                  <li>Device allows setting of voltage in 0.1V steps. It
	 *                  should return 100.
	 *                  <li>Device allows setting of ampere in 1A steps. It should
	 *                  return 1000.
	 *                  </ul>
	 * @param maximum   The maximum value that can be set e.g. 24000mA or 10000mV.
	 */
	public record Range(int offset, int precision, int maximum) {
	}

	/**
	 * Provides a consumer that sets the individual output channel of the
	 * implementation.
	 * 
	 * <p>
	 * Accept is called on SetNextWrite of {@link ChannelId#SET_OUTPUT_PERCENT}. The
	 * consumed value is already formatted to the current range and precision.
	 * 
	 * <p>
	 * Setting the value in a method like setOutputChannel(int output) directly in
	 * the implementation would look like it is a common method for other
	 * controllers
	 * 
	 * @return consumer, setting the individual output channel
	 */
	public Consumer<Integer> setOutputChannel();

	/**
	 * Range that can be used, limited by the analog IO hardware.
	 * 
	 * <p>
	 * E.g. Hardware can be set from 0 to 10V with 0.1V steps.
	 * 
	 * @return maximum range.
	 */
	public Range range();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * Holds writes of the Relay Output for debugging.
		 *
		 * <ul>
		 * <li>Type: Integer
		 * <li>Range: 0 - 100
		 * <li>Unit: %
		 * </ul>
		 */
		DEBUG_SET_OUTPUT_PERCENT(Doc.of(OpenemsType.FLOAT) //
				.unit(Unit.PERCENT)), //

		/**
		 * Set Relay Output.
		 *
		 * <p>
		 * The Output set by a controller is set in Unit.PERCENT, because the power is
		 * strongly depending on the controller and its power behavior.
		 *
		 * <ul>
		 * <li>Type: Float
		 * <li>Unit: %
		 * <li>Range: 0 - 1000
		 * </ul>
		 */
		SET_OUTPUT_PERCENT(new FloatDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.PERCENT) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_SET_OUTPUT_PERCENT)
				.<AnalogOutput>onChannelSetNextWrite((analogOutput, value) -> {

					final var offset = analogOutput.range().offset();
					final var max = analogOutput.range().maximum();

					var setOutput = offset + (max - offset) * /* Factor */(value / (float) SET_OUTPUT_ACCURACY);

					var setValidOutput = IntUtils.roundToPrecision(//
							setOutput, Round.HALF_UP, analogOutput.range().precision());

					// Set output channel.
					analogOutput.setOutputChannel().accept(setValidOutput);
				}));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the current set output as factor.
	 *
	 * @return current factor
	 */
	public default float getSetOutputPercentAsFactor() {
		return this.getDebugSetOutputPercent().orElse(0f) / (float) SET_OUTPUT_ACCURACY;
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_OUTPUT_PERCENT}.
	 *
	 * @return the Channel
	 */
	public default FloatWriteChannel getSetOutputPercentChannel() {
		return this.channel(ChannelId.SET_OUTPUT_PERCENT);
	}

	/**
	 * Sets the output value of the AnalogOutput in %. See
	 * {@link ChannelId#SET_OUTPUT_PERCENT}.
	 *
	 * @param value the next write value
	 * @throws OpenemsNamedException on error
	 */
	public default void setOutputPercent(Float value) throws OpenemsNamedException {
		this.getSetOutputPercentChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DEBUG_SET_OUTPUT_PERCENT}.
	 *
	 * @return the Channel
	 */
	public default FloatReadChannel getDebugSetOutputPercentChannel() {
		return this.channel(ChannelId.DEBUG_SET_OUTPUT_PERCENT);
	}

	/**
	 * Gets the set output value of the I/O. See
	 * {@link ChannelId#DEBUG_SET_OUTPUT_PERCENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Float> getDebugSetOutputPercent() {
		return this.getDebugSetOutputPercentChannel().value();
	}
}
