import { Component, OnInit, Input } from '@angular/core';
import { ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Websocket, Service, EdgeConfig, Edge, ChannelAddress } from 'src/app/shared/shared';

type Mode = 'MANUAL_ON' | 'MANUAL_OFF' | 'AUTOMATIC';
type RangeValues = {
    lower: number | null,
    upper: number | null
}


@Component({
    selector: ChpsocModalComponent.SELECTOR,
    templateUrl: './modal.component.html'
})
export class ChpsocModalComponent implements OnInit {

    private static readonly SELECTOR = "chpsoc-modal";

    @Input() public edge: Edge | null = null;
    @Input() public component: EdgeConfig.Component | null = null;
    @Input() public outputChannel: ChannelAddress | null = null;
    @Input() public inputChannel: ChannelAddress | null = null;

    public thresholds: RangeValues = {
        lower: 0,
        upper: 0
    }

    constructor(
        protected service: Service,
        public websocket: Websocket,
        public router: Router,
        protected translate: TranslateService,
        public modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        if (this.component != null) {
            this.thresholds.lower = this.component.properties['lowThreshold'];
            this.thresholds.upper = this.component.properties['highThreshold'];
        }
    };

    /**  
    * Updates the Mode of the Controller.
    * 
    * @param event 
    */
    updateMode(event: CustomEvent) {
        if (this.component != null) {
            let oldMode = this.component.properties.mode;
            let newMode: Mode;

            switch (event.detail.value as Mode) {
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
                this.edge.updateComponentConfig(this.websocket, this.component.id, [
                    { name: 'mode', value: newMode }
                ]).then(() => {
                    if (this.component != null) {
                        this.component.properties.mode = newMode;
                    }
                    this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                }).catch(reason => {
                    if (this.component != null) {
                        this.component.properties.mode = oldMode;
                    }
                    this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                    console.warn(reason);
                });
            }
        }
    }

    /**
    * Updates the Min-Power of force charging
    *
    * @param event
    */
    updateThresholds() {
        if (this.component != null) {
            let oldLowerThreshold = this.component.properties['lowThreshold'];
            let oldUpperThreshold = this.component.properties['highThreshold'];

            let newLowerThreshold = this.thresholds['lower'];
            let newUpperThreshold = this.thresholds['upper'];

            // prevents automatic update when no values have changed
            if (this.edge != null && (oldLowerThreshold != newLowerThreshold || oldUpperThreshold != newUpperThreshold)) {
                this.edge.updateComponentConfig(this.websocket, this.component.id, [
                    { name: 'lowThreshold', value: newLowerThreshold },
                    { name: 'highThreshold', value: newUpperThreshold }
                ]).then(() => {
                    if (this.component != null) {
                        this.component.properties['lowThreshold'] = newLowerThreshold;
                        this.component.properties['highThreshold'] = newUpperThreshold;
                    }
                    this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
                }).catch(reason => {
                    if (this.component != null) {
                        this.component.properties['lowThreshold'] = oldLowerThreshold;
                        this.component.properties['highThreshold'] = oldUpperThreshold;
                    }
                    this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
                    console.warn(reason);
                })
            }
        }
    }
}


