import { Component, Input } from '@angular/core';
import { Service, EdgeConfig, Utils } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: StorageModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class StorageModalComponent {

    @Input() public config: EdgeConfig;

    private static readonly SELECTOR = "storage-modal";

    public essComponents: EdgeConfig.Component[] = null;
    public chargerComponents: EdgeConfig.Component[] = null;

    public showPhases: boolean = false;
    public showTotal: boolean = null;
    public isOnlyChart = null;

    // referene to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
        this.service.getConfig().then(config => {
            this.essComponents = config.getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss").filter(component => !component.factoryId.includes("Ess.Cluster"));
            this.chargerComponents = config.getComponentsImplementingNature("io.openems.edge.ess.dccharger.api.EssDcCharger");
            if (this.essComponents != null && this.essComponents.length == 1) {
                this.isOnlyChart = true;
            } else if (this.essComponents.length > 1) {
                // initialize total view only if more than one ess component
                this.showTotal = false;
                this.isOnlyChart = false;
            } else {
                this.isOnlyChart = false;
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