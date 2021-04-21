package io.openems.edge.goodwe.batteryinverter;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.goodwe.common.GoodWe;

public interface GoodWeBatteryInverter
		extends GoodWe, ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, OpenemsComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		MAX_AC_EXPORT(Doc.of(OpenemsType.INTEGER)), //
		MAX_AC_IMPORT(Doc.of(OpenemsType.INTEGER)) //
		;

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
	 * Gets the Channel for {@link ChannelId#MAX_AC_EXPORT}.
	 * 
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxAcExportChannel() {
		return this.channel(ChannelId.MAX_AC_EXPORT);
	}

	/**
	 * Gets the Max AC-Export Power in [W]. Positive Values. See
	 * {@link ChannelId#MAX_AC_EXPORT}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxAcExport() {
		return this.getMaxAcExportChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_AC_EXPORT}
	 * Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxAcExport(Integer value) {
		this.getMaxAcExportChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_AC_IMPORT}.
	 * 
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxAcImportChannel() {
		return this.channel(ChannelId.MAX_AC_IMPORT);
	}

	/**
	 * Gets the Max AC-Import Power in [W]. Negative Values. See
	 * {@link ChannelId#MAX_AC_IMPORT}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxAcImport() {
		return this.getMaxAcImportChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_AC_IMPORT}
	 * Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxAcImport(Integer value) {
		this.getMaxAcImportChannel().setNextValue(value);
	}
}
