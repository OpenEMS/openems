package io.openems.common.timedata;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.openems.common.timedata.XlsxExportDetailData.XlsxExportDataEntry.HistoricTimedataSaveType;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.CurrencyConfig;

public record XlsxExportDetailData(//
		EnumMap<XlsxExportCategory, List<XlsxExportDataEntry>> data, //
		CurrencyConfig currency
) {

	public Map<HistoricTimedataSaveType, List<ChannelAddress>> getChannelsBySaveType() {
		return this.data().values().stream().flatMap(List::stream).collect(Collectors.groupingBy(
				XlsxExportDataEntry::type, Collectors.mapping(XlsxExportDataEntry::channel, Collectors.toList())));
	}

	public enum XlsxExportCategory {
		CONSUMPTION, PRODUCTION, TIME_OF_USE_TARIFF
	}

	public record XlsxExportDataEntry(//
			String alias, ChannelAddress channel, //
			HistoricTimedataSaveType type //
	) {

		public enum HistoricTimedataSaveType {
			POWER, ENERGY
		}

	}

}
