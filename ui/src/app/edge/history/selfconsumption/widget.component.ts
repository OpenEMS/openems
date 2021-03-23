import { AbstractHistoryWidget } from '../abstracthistorywidget';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, EdgeConfig } from '../../../shared/shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';

@Component({
    selector: SelfconsumptionWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class SelfconsumptionWidgetComponent extends AbstractHistoryWidget implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "selfconsumptionWidget";

    public selfconsumptionValue: number = null;
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
                    let result = response.result;
                    this.selfconsumptionValue = CurrentData.calculateSelfConsumption(result.data['_sum/GridSellActiveEnergy'],
                        result.data['_sum/ProductionActiveEnergy']);
                }).catch(() => {
                    this.selfconsumptionValue = null;
                })
            });
        })
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let channels: ChannelAddress[] = [
                new ChannelAddress('_sum', 'GridSellActiveEnergy'),
                new ChannelAddress('_sum', 'ProductionActiveEnergy'),
                new ChannelAddress('_sum', 'EssActiveDischargeEnergy')
            ];
            resolve(channels);
        });
    }
}