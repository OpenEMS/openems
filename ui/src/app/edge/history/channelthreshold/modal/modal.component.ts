import { Component, Input } from '@angular/core';
import { Service, EdgeConfig, Utils } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: ChannelthresholdModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class ChannelthresholdModalComponent {

    @Input() private controllerId: string;

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
            for (let controllerId of
                config.getComponentIdsImplementingNature("io.openems.impl.controller.channelthreshold.ChannelThresholdController")
                    .concat(config.getComponentIdsByFactory("Controller.ChannelThreshold"))) {
                this.channelthresholdComponents.push(controllerId)
            }
            if (this.channelthresholdComponents.length > 1) {
                this.showTotal = false;
            }
        })
    }

    onNotifyTotal(showTotal: boolean): void {
        this.showTotal = showTotal;
    }
}