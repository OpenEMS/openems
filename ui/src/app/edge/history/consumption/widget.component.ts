import { Component, Input, OnChanges, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ChannelAddress, Edge, EdgeConfig, Service } from '../../../shared/shared';
import { AbstractHistoryWidget } from '../abstracthistorywidget';

@Component({
    selector: ConsumptionComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class ConsumptionComponent extends AbstractHistoryWidget implements OnInit, OnChanges, OnDestroy {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "consumptionWidget";

    public data: Cumulated = null;
    public edge: Edge = null;
    public evcsComponents: EdgeConfig.Component[] = [];
    public consumptionMeterComponents: EdgeConfig.Component[] = [];
    public totalOtherEnergy: number | null = null;

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
                    this.data = response.result.data;
                    //calculate other power
                    let otherEnergy: number = 0;
                    this.evcsComponents.forEach(component => {
                        otherEnergy += this.data[component.id + '/ActiveConsumptionEnergy'] ?? 0;
                    });

                    this.consumptionMeterComponents.forEach(component => {
                        otherEnergy += (this.data[component.id + '/ActiveProductionEnergy'] ?? 0);
                    });
                    this.totalOtherEnergy = response.result.data["_sum/ConsumptionActiveEnergy"] - otherEnergy;
                }).catch(() => {
                    this.data = null;
                });
            });
        });
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {

            let channels: ChannelAddress[] = [
                new ChannelAddress('_sum', 'ConsumptionActiveEnergy')
            ];

            this.evcsComponents = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
                .filter(component =>
                    !(component.factoryId == 'Evcs.Cluster.SelfConsumption') &&
                    !(component.factoryId == 'Evcs.Cluster.PeakShaving') &&
                    !component.isEnabled == false);
            for (let component of this.evcsComponents) {
                channels.push(
                    new ChannelAddress(component.id, 'ActiveConsumptionEnergy'),
                );
            }

            this.consumptionMeterComponents = config.getComponentsImplementingNature("io.openems.edge.meter.api.ElectricityMeter")
                .filter(component => component.isEnabled && config.isTypeConsumptionMetered(component));
            for (let component of this.consumptionMeterComponents) {
                channels.push(
                    new ChannelAddress(component.id, 'ActiveProductionEnergy'),
                );
            }
            resolve(channels);
        });
    }

    public getTotalOtherEnergy(): number {
        let otherEnergy: number = 0;
        this.evcsComponents.forEach(component => {
            otherEnergy += this.data[component.id + '/ActiveConsumptionEnergy'];
        });
        this.consumptionMeterComponents.forEach(component => {
            otherEnergy += this.data[component.id + '/ActiveConsumptionEnergy'];
        });
        return this.data["_sum/ConsumptionActiveEnergy"] - otherEnergy;
    }
}

