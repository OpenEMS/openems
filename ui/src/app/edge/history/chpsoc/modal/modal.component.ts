import { Component, Input } from '@angular/core';
import { Service, EdgeConfig, Utils } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: ChpSocModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class ChpSocModalComponent {

    public chpSocComponents: string[] = [];
    public isOnlyChart: boolean = null;
    public showTotal: boolean = null;

    private static readonly SELECTOR = "chpsoc-modal";

    // referene to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
        this.service.getConfig().then(config => {
            for (let controller of config.getComponentsByFactory("Controller.CHP.SoC")) {
                this.chpSocComponents.push(controller.id);
            }
            if (this.chpSocComponents.length > 1) {
                this.isOnlyChart = false;
            } else if (this.chpSocComponents.length == 1) {
                this.isOnlyChart = true;
            }
        })
    }

    onNotifyTotal(showTotal: boolean): void {
        this.showTotal = showTotal;
    }
}