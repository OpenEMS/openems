import { Component, Input, OnChanges, OnDestroy, OnInit } from "@angular/core";
import { ActivatedRoute } from "@angular/router";
import { QueryHistoricTimeseriesDataResponse } from "src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse";
import { DefaultTypes } from "src/app/shared/type/defaulttypes";

import { ChannelAddress, Edge, EdgeConfig, Service } from "../../../shared/shared";
import { AbstractHistoryWidget } from "../abstracthistorywidget";
import { calculateActiveTimeOverPeriod } from "../shared";

@Component({
    selector: CHP_SOC_WIDGET_COMPONENT.SELECTOR,
    templateUrl: "./WIDGET.COMPONENT.HTML",
    standalone: false,
})
export class ChpSocWidgetComponent extends AbstractHistoryWidget implements OnInit, OnChanges, OnDestroy {

    private static readonly SELECTOR = "chpsocWidget";
    @Input({ required: true }) public period!: DEFAULT_TYPES.HISTORY_PERIOD;
    @Input({ required: true }) public componentId!: string;

    public activeSecondsOverPeriod: number | null = null;
    public edge: Edge | null = null;
    public component: EDGE_CONFIG.COMPONENT | null = null;

    constructor(
        public override service: Service,
        private route: ActivatedRoute,
    ) {
        super(service);
    }

    ngOnInit() {
        THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
            THIS.EDGE = edge;
            THIS.SERVICE.GET_CONFIG().then(config => {
                THIS.COMPONENT = CONFIG.GET_COMPONENT(THIS.COMPONENT_ID);
            });
        });
    }

    ngOnDestroy() {
        THIS.UNSUBSCRIBE_WIDGET_REFRESH();
    }

    ngOnChanges() {
        THIS.UPDATE_VALUES();
    }

    // Gather result & timestamps to calculate effective active time in %
    protected updateValues() {
        THIS.QUERY_HISTORIC_TIMESERIES_DATA(THIS.SERVICE.HISTORY_PERIOD.VALUE.FROM, THIS.SERVICE.HISTORY_PERIOD.VALUE.TO).then(response => {
            THIS.SERVICE.GET_CONFIG().then(config => {
                const result = (response as QueryHistoricTimeseriesDataResponse).result;
                const outputChannel = CHANNEL_ADDRESS.FROM_STRING(CONFIG.GET_COMPONENT_PROPERTIES(THIS.COMPONENT_ID)["outputChannelAddress"]);
                THIS.ACTIVE_SECONDS_OVER_PERIOD = calculateActiveTimeOverPeriod(outputChannel, result);
            });
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const outputChannel = CHANNEL_ADDRESS.FROM_STRING(CONFIG.GET_COMPONENT_PROPERTIES(THIS.COMPONENT_ID)["outputChannelAddress"]);
            const channeladdresses = [outputChannel];
            resolve(channeladdresses);
        });
    }
}

