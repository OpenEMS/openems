import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Utils, EdgeConfig } from '../../../../shared/shared';

@Component({
    selector: SinglethresholdModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class SinglethresholdModalComponent {

    @Input() public controllerId: string;
    @Input() public controller: EdgeConfig.Component;

    private static readonly SELECTOR = "channelthreshold-modal";

    public showTotal: boolean = null;
    public channelthresholdComponents: string[] = [];

    // referene to the Utils method to access via html
    public isLastElement = Utils.isLastElement;

    constructor(
        public service: Service,
        public modalCtrl: ModalController
    ) { }

    ngOnInit() {
        this.service.getConfig().then(config => {
            for (let controllerId of config.getComponentIdsByFactory("Controller.IO.ChannelSingleThreshold")) {
                this.channelthresholdComponents.push(controllerId)
            }
            if (this.channelthresholdComponents.length > 1) {
                this.showTotal = false;
            } else if (this.channelthresholdComponents.length == 1) {
                this.showTotal = null;
            }
        })
    }

    onNotifyTotal(showTotal: boolean): void {
        this.showTotal = showTotal;
    }
}