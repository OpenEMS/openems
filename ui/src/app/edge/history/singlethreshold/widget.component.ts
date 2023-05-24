import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryWidget } from '../abstracthistorywidget';
import { calculateActiveTimeOverPeriod } from '../shared';

@Component({
    selector: SinglethresholdWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class SinglethresholdWidgetComponent extends AbstractHistoryWidget implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public componentId: string;

    private static readonly SELECTOR = "singlethresholdWidget";

    public activeSecondsOverPeriod: number = null;
    public edge: Edge = null;
    public component: EdgeConfig.Component = null;

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
            });
        });
    }

    ngOnDestroy() {
        this.unsubscribeWidgetRefresh();
    }

    ngOnChanges() {
        this.updateValues();
    };

    // Gather result & timestamps to calculate effective active time in % 
    protected updateValues() {
        this.queryHistoricTimeseriesData(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).then(response => {
            this.service.getConfig().then(config => {
                let result = (response as QueryHistoricTimeseriesDataResponse).result;
                let outputChannelAddress: string | string[] = config.getComponentProperties(this.componentId)['outputChannelAddress'];
                if (typeof outputChannelAddress !== 'string') {
                    // Takes only the first output for simplicity reasons
                    outputChannelAddress = outputChannelAddress[0];
                }
                this.activeSecondsOverPeriod = calculateActiveTimeOverPeriod(ChannelAddress.fromString(outputChannelAddress), result);
            });
        });
    };

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let outputChannelAddress: string | string[] = config.getComponentProperties(this.componentId)['outputChannelAddress'];
            if (typeof outputChannelAddress === 'string') {
                resolve([ChannelAddress.fromString(outputChannelAddress)]);
            } else {
                resolve(outputChannelAddress.map(c => ChannelAddress.fromString(c)));
            }
        });
    }
}