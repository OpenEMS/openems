import { Component, Input, Output } from '@angular/core';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { HeatingElementModalComponent } from './modal/modal.component';

@Component({
    selector: HeatingElementComponent.SELECTOR,
    templateUrl: './heatingelement.component.html'
})
export class HeatingElementComponent {

    private static readonly SELECTOR = "heatingelement";


    @Input() private componentId: string;

    private edge: Edge = null;

    public controller: EdgeConfig.Component = null;
    public outputChannelPhaseOne: ChannelAddress = null;
    public outputChannelPhaseTwo: ChannelAddress = null;
    public outputChannelPhaseThree: ChannelAddress = null;


    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
        public modalController: ModalController
    ) { }

    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            this.service.getConfig().then(config => {
                this.controller = config.components[this.componentId];
                this.outputChannelPhaseOne = ChannelAddress.fromString(
                    this.controller.properties['outputChannelAddress1']);
                this.outputChannelPhaseTwo = ChannelAddress.fromString(
                    this.controller.properties['outputChannelAddress2']);
                this.outputChannelPhaseThree = ChannelAddress.fromString(
                    this.controller.properties['outputChannelAddress3']);
                edge.subscribeChannels(this.websocket, HeatingElementComponent.SELECTOR + this.componentId, [
                    this.outputChannelPhaseOne,
                    this.outputChannelPhaseTwo,
                    this.outputChannelPhaseThree
                ]);
            });
        });
    }

    getActivePhases(): number {
        let activePhases: number = 0;
        let phaseChannels = [
            this.edge.currentData['_value'].channel[this.controller.properties['outputChannelAddress1']],
            this.edge.currentData['_value'].channel[this.controller.properties['outputChannelAddress2']],
            this.edge.currentData['_value'].channel[this.controller.properties['outputChannelAddress3']]
        ];
        phaseChannels.forEach(channel => {
            if (channel == 1) {
                activePhases += 1;
            }
        })
        return activePhases;
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, HeatingElementComponent.SELECTOR + this.componentId);
        }
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: HeatingElementModalComponent,
            componentProps: {
                controller: this.controller,
                edge: this.edge,
                outputChannelPhaseOne: this.outputChannelPhaseOne,
                outputChannelPhaseTwo: this.outputChannelPhaseTwo,
                outputChannelPhaseThree: this.outputChannelPhaseThree
            }
        });
        return await modal.present();
    }
}