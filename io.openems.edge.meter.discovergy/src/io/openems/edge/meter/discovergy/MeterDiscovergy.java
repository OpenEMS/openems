package io.openems.edge.meter.discovergy;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.meter.api.AsymmetricMeter;
import io.openems.edge.meter.api.SymmetricMeter;

public interface MeterDiscovergy extends SymmetricMeter, AsymmetricMeter, OpenemsComponent, EventHandler, JsonApi {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/*
		 * Raw values from Discovergy API
		 */
		RAW_POWER(Doc.of(OpenemsType.INTEGER)), //
		RAW_POWER1(Doc.of(OpenemsType.INTEGER)), //
		RAW_POWER2(Doc.of(OpenemsType.INTEGER)), //
		RAW_POWER3(Doc.of(OpenemsType.INTEGER)), //
		RAW_VOLTAGE1(Doc.of(OpenemsType.INTEGER)), //
		RAW_VOLTAGE2(Doc.of(OpenemsType.INTEGER)), //
		RAW_VOLTAGE3(Doc.of(OpenemsType.INTEGER)), //
		RAW_ENERGY(Doc.of(OpenemsType.LONG)), //
		RAW_ENERGY1(Doc.of(OpenemsType.LONG)), //
		RAW_ENERGY2(Doc.of(OpenemsType.LONG)), //
		RAW_ENERGY_OUT(Doc.of(OpenemsType.LONG)), //
		RAW_ENERGY_OUT1(Doc.of(OpenemsType.LONG)), //
		RAW_ENERGY_OUT2(Doc.of(OpenemsType.LONG)), //

		/*
		 * StateChannels
		 */
		REST_API_FAILED(Doc.of(Level.FAULT)), //
		LAST_READING_TOO_OLD(Doc.of(Level.FAULT));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
