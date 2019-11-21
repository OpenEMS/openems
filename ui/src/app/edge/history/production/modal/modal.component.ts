import { Component } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { EdgeConfig, Service, Utils } from '../../../../shared/shared';

@Component({
    selector: ProductionModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class ProductionModalComponent {

    private static readonly SELECTOR = "production-modal";

    public config: EdgeConfig = null;
    public productionMeterComponents: EdgeConfig.Component[] = null;
    public chargerComponents: EdgeConfig.Component[] = null;
    public showTotal: boolean = null;
    public showPhases: boolean = false;
    public isOnlyChart: boolean = null;

    // referene to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
        this.service.getConfig().then(config => {
            this.config = config;
            this.productionMeterComponents = config.getComponentsImplementingNature("io.openems.edge.meter.api.SymmetricMeter").filter(component => config.isProducer(component));
            this.chargerComponents = config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger")
            if ((this.productionMeterComponents != null && this.productionMeterComponents.length > 0 && this.chargerComponents != null && this.chargerComponents.length > 0)
                || (this.productionMeterComponents != null && this.productionMeterComponents.length == 0 && this.chargerComponents != null && this.chargerComponents.length > 1)
                || (this.productionMeterComponents != null && this.productionMeterComponents.length > 1 && this.chargerComponents != null && this.chargerComponents.length == 0)) {
                this.showTotal = false;
            }
            if (((this.chargerComponents != null && this.chargerComponents.length == 1) && (this.productionMeterComponents != null && this.productionMeterComponents.length == 0))
                || ((this.productionMeterComponents != null && this.productionMeterComponents.length == 0) && (this.chargerComponents != null && this.chargerComponents.length == 1))) {
                this.isOnlyChart = true;
            } else {
                this.isOnlyChart = false;
            }
            if (this.productionMeterComponents != null && this.productionMeterComponents.length == 0) {
                this.showPhases = null;
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