import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, EdgeConfig } from '../../../shared/shared';
import { Component, Input, OnChanges, OnInit } from '@angular/core';
import { CurrentData } from 'src/app/shared/edge/currentdata';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ModalController } from '@ionic/angular';
import { AbstractHistoryWidget } from '../abstracthistorywidget';

@Component({
    selector: AutarchyWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class AutarchyWidgetComponent extends AbstractHistoryWidget implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod | null = null;

    private static readonly SELECTOR = "autarchyWidget";

    public autarchyValue: number | null = null;
    public edge: Edge | null = null;

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
        this.subscribeWidgetRefresh();
    }

    ngOnDestroy() {
        this.unsubscribeWidgetRefresh();
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
                            let result = response.result;
                            if (result.data['_sum/GridBuyActiveEnergy'] && result.data['_sum/ConsumptionActiveEnergy'] != null) {
                                this.autarchyValue = CurrentData.calculateAutarchy(result.data['_sum/GridBuyActiveEnergy'] / 1000, result.data['_sum/ConsumptionActiveEnergy'] / 1000);
                            }
                        })
                    }
                });
            }
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

