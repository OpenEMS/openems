import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { interval } from 'rxjs';
import { DefaultTypes } from '../../../../../shared/service/defaulttypes';
import { Service, Utils } from '../../../../../shared/shared';
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from './abstractsection.component';
import { UnitvaluePipe } from 'src/app/shared/pipe/unitvalue/unitvalue.pipe';
import { trigger, state, style, animate, transition } from '@angular/animations';

@Component({
    selector: '[storagesection]',
    templateUrl: './section.component.html',
    animations: [
        trigger('Discharge', [
            state('show', style({
                opacity: 1,
                transform: 'translate(0,0)'
            })),
            state('hide', style({
                opacity: 0,
                transform: 'translate(0,-10%)'
            })),
            transition('show => hide', animate('300ms')),
            transition('hide => show', animate('0ms'))
        ]),
        trigger('Charge', [
            state('show', style({
                opacity: 1,
                transform: 'translate(0,0)'
            })),
            state('hide', style({
                opacity: 0,
                transform: 'translate(0,10%)'
            })),
            transition('show => hide', animate('300ms')),
            transition('hide => show', animate('0ms'))
        ])
    ]
})
export class StorageSectionComponent extends AbstractSection implements OnInit {

    private socValue: number
    private unitpipe: UnitvaluePipe;
    public show = false;

    constructor(
        translate: TranslateService,
        service: Service,
        unitpipe: UnitvaluePipe,
    ) {
        super('Edge.Index.Energymonitor.Storage', "down", "#009846", translate, service, "Storage");
        this.unitpipe = unitpipe;
    }

    ngOnInit() {
        // let timerId = setInterval(() => {
        //     this.show = !this.show;
        // }, 850)
        // setTimeout(() => { clearInterval(timerId) }, 10000);
        setInterval(() => {
            this.show = !this.show;
        }, 850)
    }

    get stateName() {
        return this.show ? 'show' : 'hide'
    }

    protected getStartAngle(): number {
        return 136;
    }

    protected getEndAngle(): number {
        return 224;
    }

    protected getRatioType(): Ratio {
        return 'Negative and Positive [-1,1]';
    }

    public _updateCurrentData(sum: DefaultTypes.Summary): void {

        if (sum.storage.effectiveChargePower != null) {
            let arrowIndicate: number;
            if (sum.storage.effectiveChargePower > 49) {
                arrowIndicate = Utils.divideSafely(sum.storage.effectiveChargePower, sum.system.totalPower);
            } else {
                arrowIndicate = 0;
            }

            this.name = this.translate.instant('Edge.Index.Energymonitor.StorageCharge');
            super.updateSectionData(
                sum.storage.effectiveChargePower,
                sum.storage.powerRatio,
                arrowIndicate);
        } else if (sum.storage.effectiveDischargePower != null) {
            let arrowIndicate: number;
            if (sum.storage.effectiveDischargePower > 49) {
                arrowIndicate = Utils.multiplySafely(
                    Utils.divideSafely(sum.storage.effectiveDischargePower, sum.system.totalPower), -1);
            } else {
                arrowIndicate = 0;
            }
            this.name = this.translate.instant('Edge.Index.Energymonitor.StorageDischarge');
            super.updateSectionData(
                sum.storage.effectiveDischargePower,
                sum.storage.powerRatio,
                arrowIndicate);
        } else {
            this.name = this.translate.instant('Edge.Index.Energymonitor.Storage')
            super.updateSectionData(null, null, null);
        }

        this.socValue = sum.storage.soc;
        if (this.square) {
            this.square.image.image = "assets/img/" + this.getImagePath();
        }
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = (square.length / 2) * (-1);
        let y = innerRadius - 5 - square.length;
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        if (this.socValue < 20) {
            return "storage_20.png"
        } else if (this.socValue < 40) {
            return "storage_40.png"
        } else if (this.socValue < 60) {
            return "storage_60.png"
        } else if (this.socValue < 80) {
            return "storage_80.png"
        } else {
            return "storage_100.png"
        }
    }

    protected getValueText(value: number): string {
        if (value == null || Number.isNaN(value)) {
            return "";
        }
        return this.unitpipe.transform(value, 'kW');
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "50%", y1: "0%", x2: "50%", y2: "100%" });
    }

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        if (v < 8 && v != 0) {
            v = 8;
        }
        let r = radius;
        let p = {
            topLeft: { x: v * -1, y: v },
            bottomLeft: { x: v * -1, y: r * 1.2 },
            topRight: { x: v, y: v },
            bottomRight: { x: v, y: r * 1.2 },
            middleBottom: { x: 0, y: (r * 1.2) - v },
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

    protected getSvgAnimationEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        if (v < 8 && v != 0) {
            v = 8;
        }
        let r = radius;
        let animationWidth = (r * -1.2) - v;
        let p = {
            topLeft: { x: v * -1, y: v },
            bottomLeft: { x: v * -1, y: r * 1.2 },
            topRight: { x: v, y: v },
            bottomRight: { x: v, y: r * 1.2 },
            middleBottom: { x: 0, y: (r * 1.2) - v },
            middleTop: { x: 0, y: 0 }
        }
        if (ratio < 0) {
            // towards top
            p.middleTop.y = p.middleBottom.y + animationWidth * 0.1;
            p.topRight.y = p.bottomRight.y + animationWidth * 0.1;
            p.topLeft.y = p.bottomLeft.y + animationWidth * 0.1;
        } else if (ratio > 0) {
            // towards bottom
            p.bottomLeft.y = p.topLeft.y - animationWidth * 0.1;
            p.middleBottom.y = p.middleTop.y - animationWidth * 0.1 + 2 * v;
            p.bottomRight.y = p.topRight.y - animationWidth * 0.1;
            p.middleTop.y = p.middleBottom.y + animationWidth * 0.1;
        } else {
            p = null;
        }
        return p;
    }
}