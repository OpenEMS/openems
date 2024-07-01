// @ts-strict-ignore
import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryWidget } from '../abstracthistorywidget';
import { calculateActiveTimeOverPeriod } from '../shared';

@Component({
    selector: ChpSocWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html',
})
export class ChpSocWidgetComponent extends AbstractHistoryWidget implements OnInit, OnChanges, OnDestroy {

    @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
    @Input({ required: true }) public componentId!: string;

    private static readonly SELECTOR = "chpsocWidget";

    public activeSecondsOverPeriod: number = null;
    public edge: Edge = null;
    public component: EdgeConfig.Component = null;

    constructor(
        public override service: Service,
        private route: ActivatedRoute,
    ) {
        super(service);
    }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(response => {
            this.edge = response;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);
            });
        });
    }

    ngOnDestroy() {
        this.unsubscribeWidgetRefresh();
    }

    ngOnChanges() {
        this.updateValues();
    }

    // Gather result & timestamps to calculate effective active time in %
    protected updateValues() {
        this.queryHistoricTimeseriesData(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).then(response => {
            this.service.getConfig().then(config => {
                const result = (response as QueryHistoricTimeseriesDataResponse).result;
                const outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['outputChannelAddress']);
                this.activeSecondsOverPeriod = calculateActiveTimeOverPeriod(outputChannel, result);
            });
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['outputChannelAddress']);
            const channeladdresses = [outputChannel];
            resolve(channeladdresses);
        });
    }
}

