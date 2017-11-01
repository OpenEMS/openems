import { Component, OnInit, trigger, state, style, transition, animate } from '@angular/core';
import { Observable } from "rxjs/Rx";
import { TranslateService } from '@ngx-translate/core';

import { AbstractSection, SvgSquarePosition, SvgSquare, EnergyFlow, SvgEnergyFlow } from './abstractsection.component';

let PULSE = 1000;

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
            transition('one => two', animate(PULSE + 'ms')),
            transition('two => one', animate(PULSE + 'ms'))
        ])
    ]
})
export class ConsumptionSectionComponent extends AbstractSection implements OnInit {

    constructor(translate: TranslateService) {
        super('General.Consumption', "right", 46, 134, "#FDC507", translate);
    }

    ngOnInit() {
        Observable.interval(this.pulsetime)
            .subscribe(x => {
                // if (this.lastValue.absolute > 0) {
                // for (let i = 0; i < this.circles.length; i++) {
                //     setTimeout(() => {
                //         this.circles[i].switchState();
                //     }, this.pulsetime / 4 * i);
                // }
                // } else if (this.lastValue.absolute < 0) {
                // for (let i = 0; i < this.circles.length; i++) {
                //     setTimeout(() => {
                //         this.circles[this.circles.length - i - 1].switchState();
                //     }, this.pulsetime / 4 * i);
                // }
                // } else {
                // for (let i = 0; i < this.circles.length; i++) {
                //     this.circles[this.circles.length - i - 1].hide();
                // }
                // }
            })
    }
    /**
     * This method is called on every change of values.
     */
    public updateValue(valueAbsolute: number, valueRatio: number, sumRatio: number) {
        // TODO
        if (valueAbsolute < 0) {
            this.name = this.translate.instant('Device.Overview.Energymonitor.ConsumptionWarning');
        } else {
            this.name = this.translate.instant('General.Consumption');
        }
        super.updateValue(valueAbsolute, valueRatio, sumRatio);
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
            return "";
        }

        return value + " W";
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "0%", y1: "50%", x2: "100%", y2: "50%" });
    }

    protected getSvgEnergyFlow(ratio: number, r: number, v: number): SvgEnergyFlow {
        let p = {
            topLeft: { x: v, y: v * -1 },
            middleLeft: { x: 0, y: 0 },
            bottomLeft: { x: v, y: v },
            topRight: { x: r, y: v * -1 },
            bottomRight: { x: r, y: v },
            middleRight: { x: r - v, y: 0 }
        }
        if (ratio > 0) {
            // towards right
            p.topRight.x = p.topRight.x - v;
            p.middleRight.x = p.middleRight.x + v;
            p.bottomRight.x = p.bottomRight.x - v;
        }
        return p;
    }
}