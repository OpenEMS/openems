import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Utils, EdgeConfig } from '../../../../shared/shared';

@Component({
    selector: FixDigitalOutputModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class FixDigitalOutputModalComponent {

    @Input() public component: EdgeConfig.Component;
    @Input() private config: EdgeConfig;

    private static readonly SELECTOR = "fixdigitaloutput-modal";

    public showTotal: boolean = null;
    public fixDigitalOutputComponents: string[] = [];

    // referene to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
        this.config.getComponentsByFactory('Controller.Io.FixDigitalOutput').forEach(component => {
            this.fixDigitalOutputComponents.push(component.id)
        })
        if (this.fixDigitalOutputComponents.length > 1) {
            this.showTotal = false;
        } else if (this.fixDigitalOutputComponents.length == 1) {
            this.showTotal = null;
        }
    }

    onNotifyTotal(showTotal: boolean): void {
        this.showTotal = showTotal;
    }
}