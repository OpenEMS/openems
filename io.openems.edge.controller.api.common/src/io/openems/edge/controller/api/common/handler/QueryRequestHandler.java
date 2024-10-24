package io.openems.edge.controller.api.common.handler;

import java.io.IOException;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.TreeSet;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesDataRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesEnergyPerPeriodRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesEnergyRequest;
import io.openems.common.jsonrpc.request.QueryHistoricTimeseriesExportXlxsRequest;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesDataResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesEnergyPerPeriodResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesEnergyResponse;
import io.openems.common.jsonrpc.response.QueryHistoricTimeseriesExportXlsxResponse;
import io.openems.common.session.Language;
import io.openems.common.timedata.Resolution;
import io.openems.common.timedata.XlsxExportDetailData.XlsxExportDataEntry.HistoricTimedataSaveType;
import io.openems.common.timedata.XlsxExportUtil;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.jsonapi.EdgeKeys;
import io.openems.edge.common.jsonapi.JsonApi;
import io.openems.edge.common.jsonapi.JsonApiBuilder;
import io.openems.edge.timedata.api.Timedata;

@Component(service = { QueryRequestHandler.class, JsonApi.class })
public class QueryRequestHandler implements JsonApi {

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.OPTIONAL //
	)
	private volatile Timedata timedata;

	@Reference
	private ComponentManager componentManager;

	@Override
	public void buildJsonApiRoutes(JsonApiBuilder builder) {
		builder.handleRequest(QueryHistoricTimeseriesDataRequest.METHOD, call -> {
			final var data = this.getTimedata().queryHistoricData(//
					null, /* ignore Edge-ID */
					QueryHistoricTimeseriesDataRequest.from(call.getRequest()));

			return new QueryHistoricTimeseriesDataResponse(call.getRequest().getId(), data);
		});

		builder.handleRequest(QueryHistoricTimeseriesEnergyRequest.METHOD, call -> {
			final var request = QueryHistoricTimeseriesEnergyRequest.from(call.getRequest());
			final var data = this.getTimedata().queryHistoricEnergy(//
					null, /* ignore Edge-ID */
					request.getFromDate(), request.getToDate(), request.getChannels());
			return new QueryHistoricTimeseriesEnergyResponse(request.getId(), data);
		});

		builder.handleRequest(QueryHistoricTimeseriesEnergyPerPeriodRequest.METHOD, call -> {
			final var request = QueryHistoricTimeseriesEnergyPerPeriodRequest.from(call.getRequest());
			var data = this.getTimedata().queryHistoricEnergyPerPeriod(//
					null, /* ignore Edge-ID */
					request.getFromDate(), request.getToDate(), request.getChannels(), request.getResolution());
			return new QueryHistoricTimeseriesEnergyPerPeriodResponse(request.getId(), data);
		});
		builder.handleRequest(QueryHistoricTimeseriesExportXlxsRequest.METHOD, call -> {
			final var request = QueryHistoricTimeseriesExportXlxsRequest.from(call.getRequest());
			return this.handleQueryHistoricTimeseriesExportXlxsRequest(request,
					call.get(EdgeKeys.USER_KEY).getLanguage());
		});
	}

	private QueryHistoricTimeseriesExportXlsxResponse handleQueryHistoricTimeseriesExportXlxsRequest(
			QueryHistoricTimeseriesExportXlxsRequest request, Language language) throws OpenemsNamedException {
		final var powerChannels = new TreeSet<ChannelAddress>(QueryHistoricTimeseriesExportXlsxResponse.POWER_CHANNELS);
		final var energyChannels = new TreeSet<ChannelAddress>(
				QueryHistoricTimeseriesExportXlsxResponse.ENERGY_CHANNELS);
		final var detailData = XlsxExportUtil.getDetailData(this.componentManager.getEdgeConfig());
		final var channelsByType = detailData.getChannelsBySaveType();
		powerChannels.addAll(channelsByType.getOrDefault(HistoricTimedataSaveType.POWER, Collections.emptyList()));
		energyChannels.addAll(channelsByType.getOrDefault(HistoricTimedataSaveType.ENERGY, Collections.emptyList()));
		var powerData = this.timedata.queryHistoricData(null, request.getFromDate(), request.getToDate(),
				powerChannels, new Resolution(15, ChronoUnit.MINUTES));

		var energyData = this.timedata.queryHistoricEnergy(null, request.getFromDate(), request.getToDate(),
				energyChannels);
		if (powerData == null || energyData == null) {
			return null;
		}
		try {
			return new QueryHistoricTimeseriesExportXlsxResponse(request.getId(), null, request.getFromDate(),
					request.getToDate(), powerData, energyData, language, detailData);

		} catch (IOException e) {
			throw new OpenemsException("QueryHistoricTimeseriesExportXlxsRequest failed: " + e.getMessage());
		}
	}

	private final Timedata getTimedata() throws OpenemsException {
		final var currentTimedata = this.timedata;
		if (currentTimedata == null) {
			throw new OpenemsException("There is no Timedata-Service available!");
		}
		return currentTimedata;
	}

}
