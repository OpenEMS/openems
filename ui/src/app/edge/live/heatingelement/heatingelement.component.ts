import { Component, Input, Output } from '@angular/core';
import { ChannelAddress, Edge, EdgeConfig, Service, Websocket } from '../../../shared/shared';
import { ActivatedRoute } from '@angular/router';
import { ModalController } from '@ionic/angular';
import { HeatingElementModalComponent } from './modal/modal.component';
import { Subject, BehaviorSubject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';

type Level = 'LEVEL_1' | 'LEVEL_2' | 'LEVEL_3';

@Component({
    selector: HeatingElementComponent.SELECTOR,
    templateUrl: './heatingelement.component.html'
})
export class HeatingElementComponent {

    private static readonly SELECTOR = "heatingelement";


    @Input() private componentId: string;

    private edge: Edge = null;
    private stopOnDestroy: Subject<void> = new Subject<void>();

    public component: EdgeConfig.Component = null;
    public outputChannelPhaseOne: ChannelAddress = null;
    public outputChannelPhaseTwo: ChannelAddress = null;
    public outputChannelPhaseThree: ChannelAddress = null;
    public activePhases: BehaviorSubject<number> = new BehaviorSubject(0);

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
                this.component = config.components[this.componentId];
                this.outputChannelPhaseOne = ChannelAddress.fromString(
                    this.component.properties['outputChannelAddress1']);
                this.outputChannelPhaseTwo = ChannelAddress.fromString(
                    this.component.properties['outputChannelAddress2']);
                this.outputChannelPhaseThree = ChannelAddress.fromString(
                    this.component.properties['outputChannelAddress3']);
                edge.subscribeChannels(this.websocket, HeatingElementComponent.SELECTOR + this.componentId, [
                    this.outputChannelPhaseOne,
                    this.outputChannelPhaseTwo,
                    this.outputChannelPhaseThree,
                    new ChannelAddress(this.component.id, 'CountDownMinTime'),
                    new ChannelAddress(this.component.id, 'CountDownMinKwh'),
                ]);
                edge.currentData.pipe(takeUntil(this.stopOnDestroy)).subscribe(currentData => {
                    let outputChannelArray = [this.outputChannelPhaseOne, this.outputChannelPhaseTwo, this.outputChannelPhaseThree];
                    let value = 0;
                    outputChannelArray.forEach(element => {
                        if (currentData.channel[element.toString()] == 1) {
                            value += 1;
                        }
                    })
                    this.activePhases.next(value);
                })
            });
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, HeatingElementComponent.SELECTOR + this.componentId);
        }
        this.stopOnDestroy.next();
        this.stopOnDestroy.complete();
    }

    async presentModal() {
        const modal = await this.modalController.create({
            component: HeatingElementModalComponent,
            componentProps: {
                component: this.component,
                edge: this.edge,
                outputChannelPhaseOne: this.outputChannelPhaseOne,
                outputChannelPhaseTwo: this.outputChannelPhaseTwo,
                outputChannelPhaseThree: this.outputChannelPhaseThree,
                activePhases: this.activePhases
            }
        });
        return await modal.present();
    }
}