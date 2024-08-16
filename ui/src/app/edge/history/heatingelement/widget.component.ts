import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { QueryHistoricTimeseriesDataResponse } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesDataResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryWidget } from '../abstracthistorywidget';

@Component({
    selector: HeatingelementWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html',
})
export class HeatingelementWidgetComponent extends AbstractHistoryWidget implements OnInit, OnChanges, OnDestroy {

    private static readonly SELECTOR = "heatingelementWidget";
    @Input({ required: true }) public period!: DefaultTypes.HistoryPeriod;
    @Input({ required: true }) public componentId!: string;


    public component: EdgeConfig.Component | null = null;

    public activeTimeOverPeriodLevel1: number | null = null;
    public activeTimeOverPeriodLevel2: number | null = null;
    public activeTimeOverPeriodLevel3: number | null = null;

    public edge: Edge | null = null;

    constructor(
        public override service: Service,
        private route: ActivatedRoute,
    ) {
        super(service);
    }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
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

    public getCumulativeValue(channeladdress: string, response: QueryHistoricTimeseriesDataResponse) {
        const array = response.result.data[channeladdress];
        const firstValue = array.find(el => el != null) ?? 0;
        const lastValue = array.slice().reverse().find(el => el != null) ?? 0;
        return lastValue - firstValue;
    }

    protected updateValues() {
        this.queryHistoricTimeseriesData(this.service.historyPeriod.value.from, this.service.historyPeriod.value.to).then(response => {
            this.activeTimeOverPeriodLevel1 = this.getCumulativeValue(this.componentId + '/Level1CumulatedTime', response);
            this.activeTimeOverPeriodLevel2 = this.getCumulativeValue(this.componentId + '/Level2CumulatedTime', response);
            this.activeTimeOverPeriodLevel3 = this.getCumulativeValue(this.componentId + '/Level3CumulatedTime', response);
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            const channeladdresses = [
                new ChannelAddress(this.componentId, 'Level1CumulatedTime'),
                new ChannelAddress(this.componentId, 'Level2CumulatedTime'),
                new ChannelAddress(this.componentId, 'Level3CumulatedTime'),
            ];
            resolve(channeladdresses);
        });
    }
}
