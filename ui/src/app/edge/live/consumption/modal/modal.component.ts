import { Component, Input, ViewChild } from '@angular/core';
import { Edge, Service, EdgeConfig } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: ConsumptionModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class ConsumptionModalComponent {

    private static readonly SELECTOR = "consumption-modal";

    @Input() public edge: Edge | null = null
    @Input() public evcsComponents: EdgeConfig.Component[] = [];
    @Input() public consumptionMeterComponents: EdgeConfig.Component[] = [];

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    public getTotalOtherPower(): number {
        return this.currentTotalChargingPower() + this.currentTotalConsumptionMeterPower();
    }

    public currentTotalChargingPower(): number {
        return this.sumOfChannel(this.evcsComponents, "ChargePower");
    }

    public currentTotalConsumptionMeterPower(): number {
        return this.sumOfChannel(this.consumptionMeterComponents, "ActivePower");
    }

    public sumOfChannel(components: EdgeConfig.Component[], channel: String): number {
        let sum = 0;
        components.forEach(component => {
            if (this.edge != null) {
                let channelValue = this.edge.currentData.value.channel[component.id + "/" + channel];
                if (channelValue != null) {
                    sum += channelValue;
                };
            }
        });
        return sum;
    }
}