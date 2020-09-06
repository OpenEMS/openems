import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, EdgeConfig } from '../../../shared/shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { AbstractHistoryWidget } from '../abstracthistorywidget';

@Component({
    selector: ConsumptionComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class ConsumptionComponent extends AbstractHistoryWidget implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod | null = null;

    private static readonly SELECTOR = "consumptionWidget";

    public data: Cumulated | null = null;
    public edge: Edge | null = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) {
        super(service);
    }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(response => {
            this.edge = response;
        });
        this.subscribeWidgetRefresh()
    }

    ngOnDestroy() {
        this.unsubscribeWidgetRefresh()
    }

    ngOnChanges() {
        this.updateValues();
    };

    protected updateValues() {
        this.service.getConfig().then(config => {
            if (this.edge != null) {
                this.getChannelAddresses(this.edge, config).then(channels => {
                    if (this.period != null) {
                        this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
                            this.data = response.result.data;
                        })
                    }
                });
            }
        })
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let channels: ChannelAddress[] = [
                new ChannelAddress('_sum', 'ConsumptionActiveEnergy')
            ];
            resolve(channels);
        });
    }
}

