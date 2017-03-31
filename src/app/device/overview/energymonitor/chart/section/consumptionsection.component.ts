import { Component, OnInit, trigger, state, style, transition, animate } from '@angular/core';
import { AbstractSectionComponent, SvgSquarePosition, SvgSquare, CircleDirection, Circle } from './abstractsection.component';
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
                stroke: 'orange'
            })),
            transition('one => two', animate(pulsetime + 'ms')),
            transition('two => one', animate(pulsetime + 'ms'))
        ])
    ]
})
export class ConsumptionSectionComponent extends AbstractSectionComponent implements OnInit {
    constructor() {
        super("Verbrauch", 46, 134, "#FDC507");
    }

    ngOnInit() {
        Observable.interval(pulsetimeright)
            .subscribe(x => {
                for (let i = 0; i < this.circles.length; i++) {
                    setTimeout(() => {
                        this.circles[i].switchState();
                    }, pulsetime / 4 * i);
                }
            })
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
        return value + " W";
    }
}