import { ActivatedRoute } from '@angular/router';
import { calculateActiveTimeOverPeriod } from '../shared';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { AbstractHistoryWidget } from '../abstracthistorywidget';

@Component({
    selector: SinglethresholdWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class SinglethresholdWidgetComponent extends AbstractHistoryWidget implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod | null = null;
    @Input() public componentId: string = '';

    private static readonly SELECTOR = "singlethresholdWidget";

    public activeTimeOverPeriod: string | null = null;
    public edge: Edge | null = null;
    public component: EdgeConfig.Component | null = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) {
        super(service);
    }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(response => {
            this.edge = response;
            this.service.getConfig().then(config => {
                this.component = config.getComponent(this.componentId);
            })
        });
        this.subscribeWidgetRefresh()
    }

    ngOnDestroy() {
        this.unsubscribeWidgetRefresh()
    }

    ngOnChanges() {
        this.updateValues();
    };

    // Gather result & timestamps to calculate effective active time in % 
    protected updateValues() {
        this.queryHistoricTimeseriesData(this.service.historyPeriod.from, this.service.historyPeriod.to).then(response => {
            this.service.getConfig().then(config => {
                let result = (response as QueryHistoricTimeseriesDataResponse).result;
                let outputChannel = ChannelAddress.fromString(config.getComponentProperties(this.componentId)['outputChannelAddress']);
                this.activeTimeOverPeriod = calculateActiveTimeOverPeriod(outputChannel, result);
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