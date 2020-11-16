import { AbstractHistoryWidget } from '../abstracthistorywidget';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, EdgeConfig } from '../../../shared/shared';
import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

@Component({
    selector: GridComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class GridComponent extends AbstractHistoryWidget implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "gridWidget";

    public data: Cumulated = null;
    public edge: Edge = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
    ) {
        super(service);
    }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
        });
    }

    ngOnDestroy() {
        this.unsubscribeWidgetRefresh()
    }

    ngOnChanges() {
        this.updateValues();
    };

    protected updateValues() {
        this.service.getConfig().then(config => {
            this.getChannelAddresses(this.edge, config).then(channels => {
                this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
                    this.data = response.result.data;
                }).catch(() => {
                    this.data = null;
                })
            });
        })
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let channels: ChannelAddress[] = [
                new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
                new ChannelAddress('_sum', 'GridSellActiveEnergy'),
            ];
            resolve(channels);
        });
    }
}