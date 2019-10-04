import { Component } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { ChannelAddress, Edge, Service, Websocket } from '../../../shared/shared';
import { trigger, state, style, transition, animate } from '@angular/animations';

type flowState = 'void' | 'started' | 'running' | 'done'


@Component({
    selector: EnergyflowComponent.SELECTOR,
    templateUrl: './energyflow.component.html',
    animations: [
        trigger('popOverState', [
            state('show', style({
                opacity: 1,
                transform: 'translateX(0)'
            })),
            state('hide', style({
                opacity: 0,
                transform: 'translateX(-10%)'
            })),
            transition('show => hide', animate('600ms ease-out')),
            transition('hide => show', animate('1600ms ease-in'))
        ])
    ]
})
export class EnergyflowComponent {

    private static readonly SELECTOR = "energyflow";
    public state: boolean = true;


    public show = false;
    public start: boolean = true;
    public started: boolean = false;
    public running: boolean = false;
    public done: boolean = false;
    public n: number = 1;
    currentState = 'initial';
    public states: string[] = [
        'void',
        'started',
        'running',
        'done'
    ]
    public animState: flowState = 'void';

    private edge: Edge = null;


    constructor(
        public service: Service,
        private websocket: Websocket,
        private route: ActivatedRoute,
    ) { }


    toggle() {
        // this.show = !this.show;
        let timerId = setInterval(() => {
            this.show = !this.show;
        }, 850)
        setTimeout(() => { clearInterval(timerId) }, 10000);
    }
    get stateName() {
        return this.show ? 'show' : 'hide'
    }
    ngOnInit() {
        this.service.setCurrentComponent('', this.route).then(edge => {
            this.edge = edge;
            edge.subscribeChannels(this.websocket, EnergyflowComponent.SELECTOR, [
                // Ess
                new ChannelAddress('_sum', 'EssSoc'), new ChannelAddress('_sum', 'EssActivePower'), new ChannelAddress('_sum', 'EssMaxApparentPower'),
                // Grid
                new ChannelAddress('_sum', 'GridActivePower'), new ChannelAddress('_sum', 'GridMinActivePower'), new ChannelAddress('_sum', 'GridMaxActivePower'),
                // Production
                new ChannelAddress('_sum', 'ProductionActivePower'), new ChannelAddress('_sum', 'ProductionDcActualPower'), new ChannelAddress('_sum', 'ProductionAcActivePower'), new ChannelAddress('_sum', 'ProductionMaxActivePower'),
                // Consumption
                new ChannelAddress('_sum', 'ConsumptionActivePower'), new ChannelAddress('_sum', 'ConsumptionMaxActivePower')
            ]);
        });
    }

    ngOnDestroy() {
        if (this.edge != null) {
            this.edge.unsubscribeChannels(this.websocket, EnergyflowComponent.SELECTOR);
        }
    }

    changeState() {
        this.currentState = this.currentState === 'initial' ? 'final' : 'initial';
    }

    changeStatetwo() {
        setTimeout(() => {
            this.done = false;
        }, 2000);
        setTimeout(() => {
            this.running = false;
        }, 2000);
        setTimeout(() => {
            this.started = false;
        }, 2000);
        setTimeout(() => {
            this.start = false;
        }, 2000);
    }
}
