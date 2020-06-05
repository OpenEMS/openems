import { Component } from '@angular/core';
import { EdgeConfig, Service } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: ConsumptionModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class ConsumptionModalComponent {

    private static readonly SELECTOR = "consumption-modal";

    public evcsComponents: EdgeConfig.Component[] = null;
    public consumptionMeterComponents: EdgeConfig.Component[] = null;
    public showPhases: boolean = false;
    public showTotal: boolean = null;
    public isOnlyChart: boolean = null;

    constructor(
        public service: Service,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        this.service.getConfig().then(config => {
            this.evcsComponents = config.getComponentsImplementingNature("io.openems.edge.evcs.api.Evcs").filter(component => !(component.factoryId == 'Evcs.Cluster' || component.factoryId == 'Evcs.Cluster.PeakShaving' || component.factoryId == 'Evcs.Cluster.SelfConsumtion'))
            this.consumptionMeterComponents = config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter").filter(component => component.properties['type'] == 'CONSUMPTION_METERED');
            // determine if singlechart is the only chart that is shown
            // disable total option to choose for chartoptions component
            if (this.evcsComponents.length > 0) {
                this.showTotal = false;
                this.isOnlyChart = false;
            } else if (this.evcsComponents.length == 0) {
                this.isOnlyChart = true;
            }
        })
    }

    onNotifyPhases(showPhases: boolean): void {
        this.showPhases = showPhases;
    }

    onNotifyTotal(showTotal: boolean): void {
        this.showTotal = showTotal;
    }
}