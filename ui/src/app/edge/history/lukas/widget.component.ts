import { AbstractHistoryWidget } from '../abstracthistorywidget';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, EdgeConfig } from '../../../shared/shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ModalController } from '@ionic/angular';

@Component({
    selector: LukasWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class LukasWidgetComponent extends AbstractHistoryWidget implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "lukasWidget";

    public lukasValue: number | null = null;
    public edge: Edge = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
        public modalCtrl: ModalController,
    ) {
        super(service);
    }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(response => {
            this.edge = response;
        });
    }

    ngOnDestroy() {
        this.unsubscribeWidgetRefresh();
    }

    ngOnChanges() {
        this.updateValues();
    };

    protected updateValues() {
        this.service.getConfig().then(config => {
            this.getChannelAddresses(this.edge, config).then(channels => {
                this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
                    let result = response.result;
                    this.lukasValue = CurrentData.calculateAutarchy(result.data['_sum/GridBuyActiveEnergy'] / 1000, result.data['_sum/ConsumptionActiveEnergy'] / 1000);
                }).catch(() => {
                    this.lukasValue = null;
                })
            });
        })
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let channels: ChannelAddress[] = [
                new ChannelAddress('_sum', 'GridBuyActiveEnergy'),
                new ChannelAddress('_sum', 'ConsumptionActiveEnergy'),
            ];
            resolve(channels);
        });
    }
}

