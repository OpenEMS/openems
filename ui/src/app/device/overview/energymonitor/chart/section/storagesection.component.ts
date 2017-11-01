import { Component, OnInit, trigger, state, style, transition, animate } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AbstractSection, SvgSquarePosition, SvgSquare, EnergyFlow, SvgEnergyFlow } from './abstractsection.component';
import { Observable } from "rxjs/Rx";


let PULSE = 1000;

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
            transition('one => two', animate(PULSE + 'ms')),
            transition('two => one', animate(PULSE + 'ms'))
        ])
    ]
})
export class StorageSectionComponent extends AbstractSection implements OnInit {

    private state: "charging" | "discharging" | "standby" = "standby";

    constructor(translate: TranslateService) {
        super('Device.Overview.Energymonitor.Storage', "down", 136, 224, "#009846", translate);
    }

    ngOnInit() {
        Observable.interval(this.pulsetime)
            .subscribe(x => {
                if (this.state == "standby") {
                    // for (let i = 0; i < this.circles.length; i++) {
                    //     this.circles[i].hide();
                    // }
                } else if (this.state == "charging") {
                    // for (let i = 0; i < this.circles.length; i++) {
                    //     setTimeout(() => {
                    //         this.circles[i].switchState();
                    //     }, this.pulsetime / 4 * i);
                    // }
                } else if (this.state == "discharging") {
                    // for (let i = 0; i < this.circles.length; i++) {
                    //     setTimeout(() => {
                    //         this.circles[this.circles.length - i - 1].switchState();
                    //     }, this.pulsetime / 4 * i);
                    // }
                }
            })
    }

    public updateStorageValue(chargeAbsolute: number, dischargeAbsolute: number, valueRatio: number, sumChargeRatio: number, sumDischargeRatio: number) {
        if (chargeAbsolute != null && chargeAbsolute > 0) {
            this.name = this.translate.instant('Device.Overview.Energymonitor.StorageCharge')
            super.updateValue(chargeAbsolute, valueRatio, sumChargeRatio);
            this.state = "charging";
        } else {
            this.name = this.translate.instant('Device.Overview.Energymonitor.StorageDischarge')
            super.updateValue(dischargeAbsolute, valueRatio, sumDischargeRatio * -1);
            if (dischargeAbsolute > 0) {
                this.state = "discharging";
            } else {
                this.state = "standby";
            }
        }
        if (valueRatio != null) {
            this.valueText2 = valueRatio + " %";
        } else {
            this.valueText2 = "";
        }
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
            return "";
        }

        return this.lastValue.valueAbsolute + " W";
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "50%", y1: "0%", x2: "50%", y2: "100%" });
    }

    protected getSvgEnergyFlow(ratio: number, r: number, v: number): SvgEnergyFlow {
        let p = {
            topLeft: { x: v * -1, y: v },
            bottomLeft: { x: v * -1, y: r },
            topRight: { x: v, y: v },
            bottomRight: { x: v, y: r },
            middleBottom: { x: 0, y: r - v },
            middleTop: { x: 0, y: 0 }
        }
        if (ratio > 0) {
            // towards bottom
            p.bottomLeft.y = p.bottomLeft.y - v;
            p.middleBottom.y = p.middleBottom.y + v;
            p.bottomRight.y = p.bottomRight.y - v;
        }
        return p;
    }
}