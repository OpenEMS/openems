import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryWidget } from '../abstracthistorywidget';

@Component({
    selector: TimeOfUseTariffDischargeWidgetComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class TimeOfUseTariffDischargeWidgetComponent extends AbstractHistoryWidget implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;
    @Input() public componentId: string;

    private static readonly SELECTOR = "timeOfUseTariffDischargeWidget";

    public activeTimeOverPeriod: number = null;
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
            })
        });
    }

    ngOnDestroy() {
        this.unsubscribeWidgetRefresh()
    }

    ngOnChanges() {
        this.updateValues();
    };

    // Calculate active time based on a time counter
    protected updateValues() {

        this.service.getConfig().then(config => {
            this.getChannelAddresses(this.edge, config).then(channels => {
                this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
                    let result = response.result;
                    if (this.componentId + '/DelayedTime' in result.data) {
                        this.activeTimeOverPeriod = result.data[this.componentId + '/DelayedTime'];
                    }
                })
            });
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {

        return new Promise((resolve) => {
            resolve([new ChannelAddress(this.componentId, 'DelayedTime')]);
        });
    }
}