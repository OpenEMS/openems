package io.openems.edge.powerplantcontrol.api;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

@ProviderType
public interface PowerControlUnit extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		/**
		 * Holds the maximum active power feed of the grid. This value is defined like follows.
		 *
		 * <ul>
		 * <li>Interface: PowerControlUnit
		 * <li>Type: Integer
		 * <li>Unit: Watt
		 * <li>Range: zero or positive value
		 * </ul>
		 */
        MAX_ACTIVE_POWER_IMPORT(Doc.of(OpenemsType.INTEGER)//
                .unit(Unit.WATT)//
                .persistencePriority(PersistencePriority.HIGH)),
		
		/**
		 * Holds the maximum active power feed exported to the grid. This value is defined like follows.
		 *
		 * <ul>
		 * <li>Interface: PowerControlUnit
		 * <li>Type: Integer
		 * <li>Unit: Watt
		 * <li>Range: zero or positive value
		 * </ul>
		 */
        MAX_ACTIVE_POWER_EXPORT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.WATT)//
				.persistencePriority(PersistencePriority.HIGH)),

        /**
         * Holds the maximum reactive power feed exported to the grid. This value is defined like follows.
         *
         * <ul>
         * <li>Interface: PowerControlUnit
         * <li>Type: Integer
         * <li>Unit: Watt
         * <li>Range: zero or positive value
         * </ul>
         */
        MAX_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.VOLT_AMPERE_REACTIVE)//
				.persistencePriority(PersistencePriority.HIGH));



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
