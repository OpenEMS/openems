import { Component, OnInit, trigger, state, style, transition, animate } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Observable } from "rxjs/Rx";

import { AbstractSection, SvgSquarePosition, SvgSquare, EnergyFlow, SvgEnergyFlow } from './abstractsection.component';

let PULSE = 1000;

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


            transition('one => two', animate(PULSE + 'ms')),
            transition('two => one', animate(PULSE + 'ms'))
        ])
    ]
})

export class GridSectionComponent extends AbstractSection implements OnInit {
    private sellToGrid: boolean;

    constructor(translate: TranslateService) {
        super('General.Grid', "left", 226, 314, "#1d1d1d", translate);
    }

    ngOnInit() {
        Observable.interval(this.pulsetime)
            .subscribe(x => {
                if (this.sellToGrid) {
                    // for (let i = 0; i < this.circles.length; i++) {
                    //     setTimeout(() => {
                    //         this.circles[i].switchState();
                    //     }, this.pulsetime / 4 * i);
                    // }
                } else if (!this.sellToGrid) {
                    // for (let i = 0; i < this.circles.length; i++) {
                    //     setTimeout(() => {
                    //         this.circles[this.circles.length - i - 1].switchState();
                    //     }, this.pulsetime / 4 * i);
                    // }
                } else if (this.sellToGrid == null) {
                    // for (let i = 0; i < this.circles.length; i++) {
                    //     this.circles[i].hide();
                    // }
                }
            })
    }

    public updateGridValue(buyAbsolute: number, sellAbsolute: number, valueRatio: number, sumBuyRatio: number, sumSellRatio: number) {
        if (buyAbsolute != null && buyAbsolute > 0) {
            this.name = this.translate.instant('General.GridBuy');
            this.sellToGrid = false;
            super.updateValue(buyAbsolute, valueRatio, sumBuyRatio * -1);
        } else {
            this.name = this.translate.instant('General.GridSell');
            this.sellToGrid = true;
            super.updateValue(sellAbsolute, valueRatio, sumSellRatio);
        }
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
            return "";
        }

        return value + " W";
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "100%", y1: "50%", x2: "0%", y2: "50%" });
    }

    protected getSvgEnergyFlow(ratio: number, r: number, v: number): SvgEnergyFlow {
        let p = {
            topLeft: { x: r * -1, y: v * -1 },
            middleLeft: { x: r * -1 + v, y: 0 },
            bottomLeft: { x: r * -1, y: v },
            topRight: { x: v * -1, y: v * -1 },
            bottomRight: { x: v * -1, y: v },
            middleRight: { x: 0, y: 0 }
        }
        if (ratio > 0) {
            // towards left
            p.topLeft.x = p.topLeft.x + v;
            p.middleLeft.x = p.middleLeft.x - v;
            p.bottomLeft.x = p.bottomLeft.x + v;
        }
        return p;
    }
}
