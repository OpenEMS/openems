import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryWidget } from '../abstracthistorywidget';
import { calculateActiveTimeOverPeriod } from '../shared';

@Component({
    selector: FixDigitalOutputWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class FixDigitalOutputWidgetComponent extends AbstractHistoryWidget implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public componentId: string;
    private config: EdgeConfig = null;
    public component: EdgeConfig.Component = null;

    private static readonly SELECTOR = "fixDigitalOutputWidget";

    public activeSecondsOverPeriod: number = null;
    public edge: Edge = null;

    constructor(
        public override service: Service,
        private route: ActivatedRoute
    ) {
        super(service);
    }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(response => {
            this.service.getConfig().then(config => {
                this.edge = response;
                this.config = config;
                this.component = config.getComponent(this.componentId);
            });
        });
    }

    ngOnDestroy() {
        this.unsubscribeWidgetRefresh();
    }

    ngOnChanges() {
        this.updateValues();
    };

    protected updateValues() {
        // Gather result & timestamps to calculate effective active time in % 
        this.queryHistoricTimeseriesData(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).then(response => {
            let result = (response as QueryHistoricTimeseriesDataResponse).result;
            this.service.getConfig().then(config => {
                let outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['outputChannelAddress']);
                this.activeSecondsOverPeriod = calculateActiveTimeOverPeriod(outputChannel, result);
            });
        });
    };

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['outputChannelAddress']);
            let channeladdresses = [outputChannel];
            resolve(channeladdresses);
        });
    }
}
