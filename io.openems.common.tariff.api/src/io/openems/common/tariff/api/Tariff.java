package io.openems.common.tariff.api;

import java.time.Instant;
import java.util.Optional;

import io.openems.common.types.TariffData;
import io.openems.common.utils.BehaviorSubject;
import io.openems.common.utils.StringUtils;

public interface Tariff {
	/**
	 * Gets the unique identifier of this tariff provider. Used for logging
	 * purposes.
	 *
	 * @return the tariff identifier
	 */
	String getIdentifier();

	/**
	 * Gets the current tariff data. Can be null if no data is available yet. The
	 * tariff data is updated whenever new data is available.
	 *
	 * @return the tariff data behavior subject, can be null.
	 */
	BehaviorSubject<TariffData> getTariffData();

	/**
	 * Gets the status of the last tariff update. The status is updated whenever the
	 * tariff is fetched.
	 *
	 * @return the last update status behavior subject, must not be null.
	 */
	BehaviorSubject<TariffUpdateStatus> getLastUpdateStatus();

	/**
	 * Builds a compact debug log string with provider id, update status and current
	 * tariff snapshot.
	 *
	 * @param now the point in time used to resolve the current price
	 * @return formatted debug output for logging
	 */
	public default String getDebugLogOutput(Instant now) {
		final var identifier = this.getIdentifier();
		final var id = Optional.ofNullable(StringUtils.extractTextSurroundedBy(identifier, "[", "]")) //
				.orElse(identifier);

		final var tariffData = this.getTariffData().getValue();
		final var lastUpdateStatus = this.getLastUpdateStatus().getValue();

		return new StringBuilder() //
				.append(id).append(":") //
				.append(lastUpdateStatus) //
				.append(":DATA:").append(getDebugLogForTariffData(tariffData, now)) //
				.toString();
	}

	/**
	 * Formats tariff data details for debug logging.
	 *
	 * @param tariffData the tariff data to log
	 * @param now        the point in time used to resolve the current price
	 * @return formatted tariff data string, or "-" if tariff data is unavailable
	 */
	private static String getDebugLogForTariffData(TariffData tariffData, Instant now) {
		if (tariffData == null) {
			return "-";
		}

		final var curPrice = tariffData.prices().getAt(now);
		return new StringBuilder("{") //
				.append("END:").append(tariffData.prices().getEndTime()) //
				.append(",START:").append(tariffData.prices().getFirstTime()) //
				.append(",CUR:").append(curPrice != null ? curPrice : "-") //
				.append(",").append(tariffData.currency().getCurrencyCode()) //
				.append("}").toString();
	}
}
