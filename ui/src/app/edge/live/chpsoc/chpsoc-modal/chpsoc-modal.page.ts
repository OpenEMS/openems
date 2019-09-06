import { Component, OnInit, HostListener, Input, OnChanges } from '@angular/core';
import { environment } from 'src/environments/openems-backend-dev-local';
import { PopoverController, ModalController } from '@ionic/angular';
import { Router } from '@angular/router';
import { Websocket, Service, EdgeConfig, Edge, ChannelAddress } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';

type mode = 'MANUAL_ON' | 'MANUAL_OFF' | 'AUTOMATIC';


@Component({
    selector: ChpsocModalComponent.SELECTOR,
    templateUrl: './chpsoc-modal.page.html'
})
export class ChpsocModalComponent implements OnInit {

    private static readonly SELECTOR = "chpsoc-modal";

    @Input() edge: Edge;
    @Input() controller: EdgeConfig.Component = null;
    @Input() private componentId: string;

    constructor(
        protected service: Service,
        public websocket: Websocket,
        public router: Router,
        protected translate: TranslateService,
        private modalCtrl: ModalController,
    ) { }

    ngOnInit() {
        this.edge.subscribeChannels(this.websocket, ChpsocModalComponent.SELECTOR + this.componentId, [
            new ChannelAddress(this.controller.id, "Mode")
        ]);
    }

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
            ]).then(response => {
                currentController.properties.mode = newMode;
            }).catch(reason => {
                currentController.properties.mode = newMode;
                console.warn(reason);
            });
        }
    }
}


