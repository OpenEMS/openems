import { Component, OnInit, trigger, state, style, transition, animate } from '@angular/core';
import { AbstractSectionComponent, SvgSquarePosition, SvgSquare, CircleDirection, Circle } from './abstractsection.component';
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
                stroke: 'green'
            })),
            transition('one => two', animate(pulsetime + 'ms')),
            transition('two => one', animate(pulsetime + 'ms'))
        ])
    ]
})
export class StorageSectionComponent extends AbstractSectionComponent implements OnInit {
    constructor() {
        super("Speicher", 136, 224, "#009846");
    }
    // if (Ausgabeleistung2 < Ausgabeleistung1 ) {
    ngOnInit() {
        Observable.interval(pulsetimedown)
            .subscribe(x => {
                for (let i = 0; i < this.circles.length; i++) {
                    setTimeout(() => {
                        this.circles[this.circles.length - i - 1].switchState();
                    }, pulsetime / 4 * i);
                }
            })
    }
    // if (Ausgabeleistung1 = Ausgabeleistung2) { 
    // fill: white;
    // }
    // } else {
    //  ngOnInit() {
    //         Observable.interval(pulsetimedown)
    //             .subscribe(x => {
    //                 for (let i = 0; i < this.circles.length; i++) {
    //                     setTimeout(() => {
    //                         this.circles[i].switchState();
    //                     }, pulsetime / 4 * i);
    //                 }
    //             })
    //     }
    // }





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
        return value + " %";
    }
}