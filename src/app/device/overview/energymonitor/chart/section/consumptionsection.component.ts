import { Component, OnInit, trigger, state, style, transition, animate } from '@angular/core';
import { AbstractSection, SvgSquarePosition, SvgSquare, CircleDirection, Circle } from './abstractsection.component';
import { Observable } from "rxjs/Rx";


let pulsetime = 1000;
let pulsetimeright = 2000;

@Component({
    selector: '[consumptionsection]',
    templateUrl: './section.component.html',
    animations: [
        trigger('circle', [
            state('one', style({
                r: 7,
                fill: 'none',
                stroke: 'white'
            })),
            state('two', style({
                r: 7,
                fill: 'none',
                stroke: '#FDC507'
            })),
            state('three', style({
                r: 7,
                fill: 'none',
                stroke: 'none'
            })),
            transition('one => two', animate(pulsetime + 'ms')),
            transition('two => one', animate(pulsetime + 'ms'))
        ])
    ]
})
export class ConsumptionSectionComponent extends AbstractSection implements OnInit {
    value: number;
    constructor() {
        super("Verbrauch", 46, 134, "#FDC507");
    }

    ngOnInit() {
        Observable.interval(pulsetimeright)
            .subscribe(x => {
                if (this.value > 0) {
                    for (let i = 0; i < this.circles.length; i++) {
                        setTimeout(() => {
                            this.circles[i].switchState();
                        }, pulsetimeright / 4 * i);
                    }
                } else {
                    for (let i = 0; i < this.circles.length; i++) {
                        setTimeout(() => {
                            this.circles[this.circles.length - i - 1].switchState();
                        }, pulsetimeright / 4 * i);
                    }
                }
            })
    }

    public updateEnergyFlow(value: number) {
        this.value = value;
    }

    protected getCircleDirection(): CircleDirection {
        return new CircleDirection("right");
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = innerRadius - 5 - square.length;
        let y = (square.length / 2) * (-1);
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        return "consumption.png";
    }

    protected getValueText(value: number): string {
        if (value == null || Number.isNaN(value)) {
            return "Kein Wert";
        }

        return value + " W";
    }
}