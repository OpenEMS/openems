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
    public readonly CHANNEL_ID_ACTIVE_CONSUMPTION_ENERGY = "ActiveConsumptionEnergy";
    public readonly CHANNEL_SUM_CONSUMPTION_ACTIVE_ENERGY = new ChannelAddress("_sum", "ConsumptionActiveEnergy");

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

    async ngOnInit() {
        await this.service.setCurrentComponent('', this.route).then(response => {
            this.edge = response;
        });
    }

    ngOnDestroy() {
        this.unsubscribeWidgetRefresh()
    }

    async ngOnChanges() {
      await this.updateValues();
    };

    public updateValues() : Promise<void>{
        return this.service.getConfig().then(config => {
            return this.getChannelAddresses(this.edge, config).then(channels => {
                return this.service.queryEnergy(this.period.from, this.period.to, channels).then(response => {
                    this.data = response.result.data;
                    //calculate other power
                    let otherEnergy: number = 0;
                    this.evcsComponents.forEach(component => {
                        otherEnergy += this.data[this.getChannelAddressString(component, this.CHANNEL_ID_ACTIVE_CONSUMPTION_ENERGY)] ?? 0;
                    })

                    this.consumptionMeterComponents.forEach(component => {
                        otherEnergy += (this.data[this.getChannelAddressString(component, this.CHANNEL_ID_ACTIVE_CONSUMPTION_ENERGY)] ?? 0);
                    });
                    this.totalOtherEnergy = response.result.data[this.CHANNEL_SUM_CONSUMPTION_ACTIVE_ENERGY.toString()] - otherEnergy;
                }).catch(() => {
                    this.data = null;
                })
            });
        })
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {

            let channels: ChannelAddress[] = [this.CHANNEL_SUM_CONSUMPTION_ACTIVE_ENERGY]

            this.evcsComponents = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs")
                .filter(component =>
                    !(component.factoryId == 'Evcs.Cluster.SelfConsumption') &&
                    !(component.factoryId == 'Evcs.Cluster.PeakShaving') &&
                    !component.isEnabled == false);
            for (let component of this.evcsComponents) {
                channels.push(
                    new ChannelAddress(component.id, this.CHANNEL_ID_ACTIVE_CONSUMPTION_ENERGY),
                )
            }

            this.consumptionMeterComponents = config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter")
                .filter(component => component.isEnabled && config.isTypeConsumptionMetered(component));
            for (let component of this.consumptionMeterComponents) {
                channels.push(
                    new ChannelAddress(component.id, this.CHANNEL_ID_ACTIVE_CONSUMPTION_ENERGY),
                )
            }
            resolve(channels);
        });
    }

    public getChannelAddressString(component: EdgeConfig.Component, channelId: string): string {
      return new ChannelAddress(component.id, channelId).toString();
    }

    public getTotalOtherEnergy(): number {
        let otherEnergy: number = 0;
        this.evcsComponents.forEach(component => {
            otherEnergy += this.data[this.getChannelAddressString(component, this.CHANNEL_ID_ACTIVE_CONSUMPTION_ENERGY)];
        })
        this.consumptionMeterComponents.forEach(component => {
            otherEnergy += this.data[this.getChannelAddressString(component, this.CHANNEL_ID_ACTIVE_CONSUMPTION_ENERGY)];
        })
        return this.data[this.CHANNEL_SUM_CONSUMPTION_ACTIVE_ENERGY.toString()] - otherEnergy;
    }
}

