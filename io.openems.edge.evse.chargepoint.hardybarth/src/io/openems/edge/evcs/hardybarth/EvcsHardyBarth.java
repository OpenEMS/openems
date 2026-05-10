package io.openems.edge.evcs.hardybarth;

import static io.openems.common.types.OpenemsType.FLOAT;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.STRING;
import static io.openems.edge.common.channel.ChannelUtils.setValue;
import static java.lang.Math.round;

import java.util.function.Function;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.evcs.api.DeprecatedEvcs;
import io.openems.edge.evcs.api.Evcs;
import io.openems.edge.evcs.api.ManagedEvcs;
import io.openems.edge.evse.chargepoint.hardybarth.common.HardyBarth.Path;
import io.openems.edge.evse.chargepoint.hardybarth.common.HardyBarth.PathProvider;
import io.openems.edge.meter.api.ElectricityMeter;

public interface EvcsHardyBarth extends OpenemsComponent, Evcs, ManagedEvcs, DeprecatedEvcs, ElectricityMeter {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId, PathProvider {
		ENERGY_SESSION(Doc.of(INTEGER)), //
		RAW_ENERGY_SESSION(Doc.of(STRING) //
				.onChannelSetNextValue((hb, value) -> {
					if (value != null) {
						var chargedata = TypeUtils.<String>getAsType(STRING, value).split("\\|");
						if (chargedata.length == 3) {
							setValue(hb, EvcsHardyBarth.ChannelId.ENERGY_SESSION,
									round(TypeUtils.<Float>getAsType(FLOAT, chargedata[2]) * 1000));
						}
					}
				}), "secc", "port0", "salia", "chargedata"), //
		;

		private final Doc doc;
		private final Path path;

		private ChannelId(Doc doc, String... jsonPaths) {
			this(doc, value -> value, jsonPaths);
		}

		private ChannelId(Doc doc, Function<Object, Object> converter, String... jsonPaths) {
			this.doc = doc;
			this.path = new Path(converter, jsonPaths);
		}

		@Override
		public Doc doc() {
			return this.doc;
		}

		@Override
		public Path getPath() {
			return this.path;
		}
	}
}
