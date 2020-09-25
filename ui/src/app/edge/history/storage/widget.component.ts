import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, EdgeConfig } from '../../../shared/shared';
import { Component, Input, OnInit, OnChanges } from '@angular/core';
import { Cumulated } from 'src/app/shared/jsonrpc/response/queryHistoricTimeseriesEnergyResponse';
import { DefaultTypes } from 'src/app/shared/service/defaulttypes';
import { ModalController } from '@ionic/angular';
import { AbstractHistoryWidget } from '../abstracthistorywidget';

@Component({
    selector: StorageComponent.SELECTOR,
    templateUrl: './widget.component.html'
})
export class StorageComponent extends AbstractHistoryWidget implements OnInit, OnChanges {

    @Input() public period: DefaultTypes.HistoryPeriod;

    private static readonly SELECTOR = "storageWidget";

    public data: Cumulated = null;
    public edge: Edge = null;
    public essComponents: EdgeConfig.Component[] = [];

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
                })
            });
        })
    }

    protected getChannelAddresses(edge: Edge, config: EdgeConfig): Promise<ChannelAddress[]> {
        return new Promise((resolve) => {
            let channels: ChannelAddress[] = [
                new ChannelAddress('_sum', 'EssActiveChargeEnergy'),
                new ChannelAddress('_sum', 'EssActiveDischargeEnergy'),
            ];
            this.essComponents = config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss").filter(component => !component.factoryId.includes("Ess.Cluster") && component.isEnabled);
            this.essComponents.forEach(component => {
                channels.push(
                    new ChannelAddress(component.id, 'ActiveChargeEnergy'),
                    new ChannelAddress(component.id, 'ActiveDischargeEnergy'),
                )
            })
            resolve(channels);
        });
    }
}