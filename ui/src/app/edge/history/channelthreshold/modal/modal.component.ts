import { Component } from '@angular/core';
import { Service, EdgeConfig } from '../../../../shared/shared';
import { ModalController } from '@ionic/angular';

@Component({
    selector: ChannelthresholdModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class ChannelthresholdModalComponent {

    private static readonly SELECTOR = "channelthreshold-modal";

    public showTotal: boolean = null;
    public isOnlyChart: boolean = null;
    public channelthresholdComponents: string[] = [];

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
                this.isOnlyChart = false;
            } else if (this.channelthresholdComponents.length == 1) {
                this.isOnlyChart = true;
            }
        })
    }

    onNotifyTotal(showTotal: boolean): void {
        this.showTotal = showTotal;
    }
}