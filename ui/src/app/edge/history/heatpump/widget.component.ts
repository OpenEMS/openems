import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryWidget } from '../abstracthistorywidget';

@Component({
    selector: HeatpumpWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class HeatpumpWidgetComponent extends AbstractHistoryWidget implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public componentId: string;

    private static readonly SELECTOR = "heatpumpWidget";

    public component: EdgeConfig.Component | null = null;

    public activeTimeOverPeriodForceOn: number | null = null;
    public activeTimeOverPeriodRegular: number | null = null;
    public activeTimeOverPeriodRecommendation: number | null = null;
    public activeTimeOverPeriodLock: number | null = null;

    public edge: Edge = null;

    constructor(
        public service: Service,
        private route: ActivatedRoute,
        public modalCtrl: ModalController
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
    };

    protected updateValues() {
        this.service.getConfig().then(config => {
            this.getChannelAddresses(this.edge, config).then(channels => {
                this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
                    let result = response.result;
                    if (this.componentId + '/ForceOnStateTime' in result.data) {
                        this.activeTimeOverPeriodForceOn = result.data[this.componentId + '/ForceOnStateTime'];
                    }
                    if (this.componentId + '/RegularStateTime' in result.data) {
                        this.activeTimeOverPeriodRegular = result.data[this.componentId + '/RegularStateTime'];
                    }
                    if (this.componentId + '/RecommendationStateTime' in result.data) {
                        this.activeTimeOverPeriodRecommendation = result.data[this.componentId + '/RecommendationStateTime'];
                    }
                    if (this.componentId + '/LockStateTime' in result.data) {
                        this.activeTimeOverPeriodLock = result.data[this.componentId + '/LockStateTime'];
                    }
                });
            });
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let channels: ChannelAddress[] = [
                new ChannelAddress(this.componentId, 'ForceOnStateTime'),
                new ChannelAddress(this.componentId, 'RegularStateTime'),
                new ChannelAddress(this.componentId, 'RecommendationStateTime'),
                new ChannelAddress(this.componentId, 'LockStateTime')
            ];
            resolve(channels);
        });
    }
}

