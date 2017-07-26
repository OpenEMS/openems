import { Component, OnInit, trigger, state, style, transition, animate } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from "rxjs/Rx";

import { AbstractSection, SvgSquarePosition, SvgSquare, CircleDirection, Circle } from './abstractsection.component';

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
                stroke: '#1d1d1d'
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

export class GridSectionComponent extends AbstractSection implements OnInit {
    private sellToGrid: boolean;

    constructor(translate: TranslateService) {
        super('General.Grid', 226, 314, "#1d1d1d", translate);
    }

    ngOnInit() {
        Observable.interval(pulsetimeleft)
            .subscribe(x => {
                if (this.sellToGrid) {
                    for (let i = 0; i < this.circles.length; i++) {
                        setTimeout(() => {
                            this.circles[i].switchState();
                        }, pulsetimeleft / 4 * i);
                    }
                } else if (!this.sellToGrid) {
                    for (let i = 0; i < this.circles.length; i++) {
                        setTimeout(() => {
                            this.circles[this.circles.length - i - 1].switchState();
                        }, pulsetimeleft / 4 * i);
                    }
                } else if (this.sellToGrid == null) {
                    for (let i = 0; i < this.circles.length; i++) {
                        this.circles[i].hide();
                    }
                }
            })
    }

    public updateValue(absolute: number, ratio: number) {
        if (absolute < 0) {
            this.name = this.translate.instant('General.GridSell');
            this.sellToGrid = true;
            absolute *= -1;
        } else if (absolute > 0) {
            this.name = this.translate.instant('General.GridBuy');
            this.sellToGrid = false;
        } else {
            this.name = this.translate.instant('General.Grid');
            this.sellToGrid = null;
        }
        super.updateValue(absolute, ratio);
    }

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
        if (value == null || Number.isNaN(value)) {
            return "Kein Wert";
        }

        return value + " W";
    }
}
