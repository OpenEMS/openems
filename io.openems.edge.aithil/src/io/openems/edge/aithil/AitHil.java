package io.openems.edge.aithil;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface AitHil extends OpenemsComponent {

	public static class PByF {
		public final float p;
		public final float f;

		public PByF(float p, float f) {
			this.p = p;
			this.f = f;
		}

	}

	public static class FreqWattCrv {
		public final boolean enabled;
		public final PByF[] curve;

		public FreqWattCrv(boolean enabled, PByF[] curve) {
			this.enabled = enabled;
			this.curve = curve;
		}
	}

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
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

	public void applyFreqWattCrv(FreqWattCrv freqWattCrv) throws OpenemsNamedException;

}
