import { Component, OnInit, trigger, state, style, transition, animate } from '@angular/core';
import { AbstractSection, SvgSquarePosition, SvgSquare, CircleDirection, Circle } from './abstractsection.component';
import { Observable } from "rxjs/Rx";


let pulsetime = 1000;
let pulsetimedown = 2000;

@Component({
    selector: '[storagesection]',
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
                stroke: '#009846'
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
export class StorageSectionComponent extends AbstractSection implements OnInit {

    constructor() {
        super("Speicher", 136, 224, "#009846");
    }

    ngOnInit() {
        Observable.interval(pulsetimedown)
            .subscribe(x => {
                if (this.lastValue.absolute > 0) {
                    for (let i = 0; i < this.circles.length; i++) {
                        setTimeout(() => {
                            this.circles[i].switchState();
                        }, pulsetime / 4 * i);
                    }
                } else if (this.lastValue.absolute == 0) {
                    for (let i = 0; i < this.circles.length; i++) {
                        this.circles[i].hide();
                    }
                } else {
                    for (let i = 0; i < this.circles.length; i++) {
                        setTimeout(() => {
                            this.circles[i].switchState();
                        })
                    }
                }
            })
    }

    protected getCircleDirection(): CircleDirection {
        return new CircleDirection("down");
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = (square.length / 2) * (-1);
        let y = innerRadius - 5 - square.length;
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        return "storage.png";
    }

    protected getValueText(value: number): string {
        if (value == null || Number.isNaN(value)) {
            return "Kein Wert";
        }

        return this.lastValue.ratio + " %";
    }
}