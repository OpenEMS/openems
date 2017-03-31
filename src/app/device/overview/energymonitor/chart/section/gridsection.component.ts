import { Component, OnInit, trigger, state, style, transition, animate } from '@angular/core';
import { AbstractSectionComponent, SvgSquarePosition, SvgSquare, CircleDirection, Circle } from './abstractsection.component';
import { Observable } from "rxjs/Rx";

let pulsetime = 1000;
let pulsetimeleft = 2000;

@Component({
    selector: '[gridsection]',
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
                stroke: 'black'
            })),
            transition('one => two', animate(pulsetime + 'ms')),
            transition('two => one', animate(pulsetime + 'ms'))
        ])
    ]
})

export class GridSectionComponent extends AbstractSectionComponent implements OnInit {
    constructor() {
        super("Netz", 226, 314, "#1d1d1d");
    }
    // if ( ... <= 0 ) { 
    ngOnInit() {
        Observable.interval(pulsetimeleft)
            .subscribe(x => {
                for (let i = 0; i < this.circles.length; i++) {
                    setTimeout(() => {
                        this.circles[this.circles.length - i - 1].switchState();
                    }, pulsetime / 4 * i);
                }
            })
    }

    // if (... = ...) {
    //fill: 'white',
    // }
    // } else {
    // ngOnInit() {
    //     Observable.interval(pulsetimeleft)
    //         .subscribe(x => {
    //             for (let i = 0; i < this.circles.length; i++) {
    //                 setTimeout(() => {
    //                     this.circles[i].switchState();
    //                 }, pulsetime / 4 * i);
    //             }
    //         })
    // }

    // }


    protected getCircleDirection(): CircleDirection {
        return new CircleDirection("left");
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = (innerRadius - 5) * (-1);
        let y = (square.length / 2) * (-1);
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        return "grid.png";
    }

    public getValueRatio(value: number) {
        if (value > 50) {
            return 50;
        } else if (value < -50) {
            return 50;
        }
        return value;
    }

    protected getValueStartAngle(): number {
        return (this.startAngle + this.endAngle) / 2;
    }

    protected getValueText(value: number): string {
        return value + " W";
    }
}
