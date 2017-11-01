import { Component, OnInit, trigger, state, style, transition, animate } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from "rxjs/Rx";

import { AbstractSection, SvgSquarePosition, SvgSquare, EnergyFlow, SvgEnergyFlow } from './abstractsection.component';

let PULSE = 1000;

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
            transition('one => two', animate(PULSE + 'ms')),
            transition('two => one', animate(PULSE + 'ms'))
        ])
    ]
})
export class ProductionSectionComponent extends AbstractSection implements OnInit {

    constructor(translate: TranslateService) {
        super('General.Production', "up", 316, 404, "#008DD2", translate);
    }

    ngOnInit() {
        Observable.interval(this.pulsetime)
            .subscribe(x => {
                // if (this.lastValue.absolute > 0) {
                // for (let i = 0; i < this.circles.length; i++) {
                //     setTimeout(() => {
                //         this.circles[this.circles.length - i - 1].switchState();
                //     }, this.pulsetime / 4 * i);
                // }
                // } else if (this.lastValue.absolute == 0) {
                // for (let i = 0; i < this.circles.length; i++) {
                //     this.circles[this.circles.length - i - 1].hide();
                // }
                // } else {
                // for (let i = 0; i < this.circles.length; i++) {
                //     this.circles[this.circles.length - i - 1].switchState();
                // }
                // }
            })
    }

    /**
     * This method is called on every change of values.
     */
    public updateValue(valueAbsolute: number, valueRatio: number, sumRatio: number) {
        super.updateValue(valueAbsolute, valueRatio, sumRatio * -1)
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
        if (value == null || Number.isNaN(value)) {
            return "";
        }

        return value + " W";
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "50%", y1: "100%", x2: "50%", y2: "0%" });
    }

    protected getSvgEnergyFlow(ratio: number, r: number, v: number): SvgEnergyFlow {
        let p = {
            topLeft: { x: v * -1, y: r * -1 },
            bottomLeft: { x: v * -1, y: v * -1 },
            topRight: { x: v, y: r * -1 },
            bottomRight: { x: v, y: v * -1 },
            middleBottom: { x: 0, y: 0 },
            middleTop: { x: 0, y: r * -1 + v }
        }
        if (ratio > 0) {
            // towards top
            p.topLeft.y = p.topLeft.y + v;
            p.middleTop.y = p.middleTop.y - v;
            p.topRight.y = p.topRight.y + v;
        }
        return p;
    }
}