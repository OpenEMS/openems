package io.openems.edge.goodwe.stsbox;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.goodwe.common.enums.GensetInstalledStatus;
import io.openems.edge.goodwe.common.enums.MultiplexingMode;

public interface GoodWeStsBox extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		/**
		 * ESC Port Multiplexing Mode.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: none
		 * <li>Range:
		 * <ul>
		 * <li>0 = genset mode (default)
		 * <li>1 = Large load mode
		 * <li>2 = Normal BACKUP mode
		 * </ul>
		 * </ul>
		 */
		PORT_MUTLIPLEXING_MODE(Doc.of(MultiplexingMode.values())//
				.accessMode(AccessMode.READ_WRITE)),

		/**
		 * genset Installed.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: none
		 * <li>Range:
		 * <ul>
		 * <li>0 = true (genset installed)
		 * <li>2 = false (genset not installed)
		 * </ul>
		 * </ul>
		 */
		GENSET_START_MODE_SELECTION(Doc.of(GensetInstalledStatus.values())//
				.accessMode(AccessMode.READ_WRITE)),

		/**
		 * genset Rated Power.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#WATT}
		 * <li>Range: 0 – 50000 W
		 * </ul>
		 */
		GENSET_RATED_POWER(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.WATT)),

		/**
		 * Delay Before Load (Preheating Time).
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#SECONDS}
		 * <li>Range: 10 – 300 s
		 * </ul>
		 */
		DELAY_BEFORE_LOAD(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE).unit(Unit.SECONDS)),

		/**
		 * genset Run Time.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#HOUR}
		 * <li>Range: 0 – 1440 m
		 * </ul>
		 */
		GENSET_RUN_TIME(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE).unit(Unit.MINUTE)),

		/**
		 * Battery Charging from genset (One-Click Enable).
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: none
		 * <li>Range:
		 * <ul>
		 * <li>0 = disabled
		 * <li>1 = enabled
		 * </ul>
		 * </ul>
		 */
		ONE_CLICK_ENABLE(Doc.of(EnableDisable.values())//
				.accessMode(AccessMode.READ_WRITE)),

		/**
		 * genset Charge Limit.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#PERCENT}
		 * <li>Range: 0 – 100 %
		 * </ul>
		 */
		GENSET_CHARGE_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT)),

		/**
		 * Battery Charge Start State of Charge (SOC).
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#PERCENT}
		 * <li>Range: 20 – 90 %
		 * </ul>
		 */
		OPEN_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT)),

		/**
		 * Battery Charge Stop State of Charge (SOC).
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#PERCENT}
		 * <li>Range: 40 – 95 %
		 * </ul>
		 */
		CLOSED_VOLTAGE(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE).unit(Unit.PERCENT)),

		/**
		 * genset Upper Voltage Limit.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#VOLT}
		 * <li>Range: typically up to 280 V
		 * </ul>
		 */
		GENSET_UPPER_VOLTAGE_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.VOLT)),

		/**
		 * genset Lower Voltage Limit.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#VOLT}
		 * <li>Range: typically down to 80 V
		 * </ul>
		 */
		GENSET_LOWER_VOLTAGE_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.VOLT)),

		/**
		 * genset Upper Frequency Limit.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#HERTZ}
		 * <li>Range: typically up to 65 Hz
		 * </ul>
		 */
		GENSET_UPPER_FREQUENCY_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.HERTZ)),

		/**
		 * genset Lower Frequency Limit.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * <li>Unit: {@link Unit#HERTZ}
		 * <li>Range: typically down to 45 Hz
		 * </ul>
		 */
		GENSET_LOWER_FREQUENCY_LIMIT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)//
				.unit(Unit.HERTZ)),

		/**
		 * Version.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * </ul>
		 */
		VERSION(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Subversion.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#INTEGER}
		 * </ul>
		 */
		SUB_VERSION(Doc.of(OpenemsType.INTEGER)//
				.persistencePriority(PersistencePriority.HIGH)),

		/**
		 * Serial number.
		 *
		 * <ul>
		 * <li>Type: {@link OpenemsType#STRING}
		 * </ul>
		 */
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING)//
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
