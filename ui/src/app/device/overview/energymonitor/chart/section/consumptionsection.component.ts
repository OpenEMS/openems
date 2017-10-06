import { Component, OnInit, trigger, state, style, transition, animate } from '@angular/core';
import { Observable } from "rxjs/Rx";
import { TranslateService } from '@ngx-translate/core';

import { AbstractSection, SvgSquarePosition, SvgSquare, CircleDirection, Circle } from './abstractsection.component';

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
        super('General.Consumption', 46, 134, "#FDC507", translate);
    }

    ngOnInit() {
        Observable.interval(this.pulsetime)
            .subscribe(x => {
                if (this.lastValue.absolute > 0) {
                    for (let i = 0; i < this.circles.length; i++) {
                        setTimeout(() => {
                            this.circles[i].switchState();
                        }, this.pulsetime / 4 * i);
                    }
                } else if (this.lastValue.absolute < 0) {
                    for (let i = 0; i < this.circles.length; i++) {
                        setTimeout(() => {
                            this.circles[this.circles.length - i - 1].switchState();
                        }, this.pulsetime / 4 * i);
                    }
                } else {
                    for (let i = 0; i < this.circles.length; i++) {
                        this.circles[this.circles.length - i - 1].hide();
                    }
                }
            })
    }
    /**
     * This method is called on every change of values.
     */
    public updateValue(absolute: number, ratio: number) {
        // TODO
        if (absolute < 0) {
            this.name = this.translate.instant('Device.Overview.Energymonitor.ConsumptionWarning');
        } else {
            this.name = this.translate.instant('General.Consumption');
        }
        super.updateValue(absolute, ratio);
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
            return "";
        }

        return value + " W";
    }
}