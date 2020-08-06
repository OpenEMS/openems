import { Component, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Service, Utils, EdgeConfig } from '../../../../shared/shared';

@Component({
    selector: ChannelthresholdModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class ChannelthresholdModalComponent {

    @Input() public component: EdgeConfig.Component;
    @Input() private config: EdgeConfig;

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
        for (let componentId of
            this.config.getComponentIdsImplementingNature("io.openems.impl.controller.channelthreshold.ChannelThresholdController")
                .concat(this.config.getComponentIdsByFactory("Controller.ChannelThreshold"))) {
            this.channelthresholdComponents.push(componentId)
        }
        if (this.channelthresholdComponents.length > 1) {
            this.showTotal = false;
        } else if (this.channelthresholdComponents.length == 1) {
            this.showTotal = null;
        }
    }

    onNotifyTotal(showTotal: boolean): void {
        this.showTotal = showTotal;
    }
}