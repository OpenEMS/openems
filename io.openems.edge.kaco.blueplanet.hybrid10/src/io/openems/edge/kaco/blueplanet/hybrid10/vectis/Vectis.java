package io.openems.edge.kaco.blueplanet.hybrid10.vectis;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumDoc;

public interface Vectis {

	public final static EnumDoc SENSOR_CONFIG_DOC = new EnumDoc(SensorConfig.values());

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		VECTIS_STATUS(Doc.of(VectisStatus.values())), //
		SENSOR_CONFIG(SENSOR_CONFIG_DOC) //
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
}