import { Component, OnInit, trigger, state, style, transition, animate } from '@angular/core';
import { AbstractSection, SvgSquarePosition, SvgSquare, CircleDirection, Circle } from './abstractsection.component';
import { Observable } from "rxjs/Rx";


let pulsetime = 500;
let pulsetimeup = 2000;

@Component({
    selector: '[productionsection]',
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
                stroke: '#008DD2'
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
export class ProductionSectionComponent extends AbstractSection implements OnInit {
    value: number;
    constructor() {
        super("Erzeugung", 316, 404, "#008DD2");
    }

    ngOnInit() {
        Observable.interval(pulsetimeup)
            .subscribe(x => {
                if (this.value > 0) {
                    for (let i = 0; i < this.circles.length; i++) {
                        setTimeout(() => {
                            this.circles[this.circles.length - i - 1].switchState();
                        }, pulsetime / 4 * i);
                    }
                } else {
                    for (let i = 0; i < this.circles.length; i++) {
                        setTimeout(() => {
                        }, );
                    }
                }
            })
    }

    public updateEnergyFlow(value: number) {
        this.value = value;
    }

    protected getCircleDirection(): CircleDirection {
        return new CircleDirection("up");
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = (square.length / 2) * (-1);
        let y = (innerRadius - 5) * (-1);
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        return "production.png";
    }

    protected getValueText(value: number): string {
        return value + " W";
    }
}