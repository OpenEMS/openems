import { Component, OnInit, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { Websocket, Service, EdgeConfig, Edge, ChannelAddress } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';
import { RangeValue } from '@ionic/core';

type mode = 'MANUAL_ON' | 'MANUAL_OFF' | 'AUTOMATIC';


@Component({
    selector: ChpsocModalComponent.SELECTOR,
    templateUrl: './modal.page.html'
})
export class ChpsocModalComponent implements OnInit {

    private static readonly SELECTOR = "chpsoc-modal";

    @Input() public edge: Edge;
    @Input() public controller: EdgeConfig.Component;
    @Input() public componentId: string;
    @Input() public outputChannel: ChannelAddress;
    @Input() public inputChannel: ChannelAddress;

    public thresholds: RangeValue = {
        lower: null,
        upper: null
    };

    constructor(
        protected service: Service,
        public websocket: Websocket,
        public router: Router,
        protected translate: TranslateService,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        this.edge.subscribeChannels(this.websocket, ChpsocModalComponent.SELECTOR + this.componentId, [
            new ChannelAddress(this.controller.id, "Mode")
        ]);
        this.thresholds['lower'] = this.controller.properties['lowThreshold'];
        this.thresholds['upper'] = this.controller.properties['highThreshold'];
    };

    ngOnDestroy() {
        this.edge.unsubscribeChannels(this.websocket, ChpsocModalComponent.SELECTOR + this.componentId);
    }

    cancel() {
        this.modalCtrl.dismiss();
    }



    /**  
    * Updates the Charge-Mode of the EVCS-Controller.
    * 
    * @param event 
    */
    updateMode(event: CustomEvent, currentController: EdgeConfig.Component) {
        let oldMode = currentController.properties.mode;
        let newMode: mode;

        switch (event.detail.value) {
            case 'MANUAL_ON':
                newMode = 'MANUAL_ON';
                break;
            case 'MANUAL_OFF':
                newMode = 'MANUAL_OFF';
                break;
            case 'AUTOMATIC':
                newMode = 'AUTOMATIC';
                break;
        }

        if (this.edge != null) {
            this.edge.updateComponentConfig(this.websocket, currentController.id, [
                { name: 'mode', value: newMode }
            ]).then(() => {
                currentController.properties.mode = newMode;
            }).catch(reason => {
                currentController.properties.mode = oldMode;
                console.warn(reason);
            });
        }
    }

    /**
    * Updates the Min-Power of force charging
    *
    * @param event
    */
    updateThresholds(currentController: EdgeConfig.Component) {
        let oldLowerThreshold = currentController.properties['lowThreshold'];
        let oldUpperThreshold = currentController.properties['highThreshold'];

        let newLowerThreshold = this.thresholds['lower'];
        let newUpperThreshold = this.thresholds['upper'];

        if (this.edge != null) {
            this.edge.updateComponentConfig(this.websocket, currentController.id, [
                { name: 'lowThreshold', value: newLowerThreshold },
                { name: 'highThreshold', value: newUpperThreshold }
            ]).then(() => {
                currentController.properties['lowThreshold'] = newLowerThreshold;
                currentController.properties['highThreshold'] = newUpperThreshold;
            }).catch(reason => {
                currentController.properties['lowThreshold'] = oldLowerThreshold;
                currentController.properties['highThreshold'] = oldUpperThreshold;
                console.warn(reason);
            })
        }
    }
}


