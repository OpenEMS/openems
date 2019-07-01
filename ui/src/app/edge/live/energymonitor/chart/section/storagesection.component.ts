import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { interval } from 'rxjs';
import { AbstractSection, EnergyFlow, SvgEnergyFlow, SvgSquare, SvgSquarePosition, Ratio } from './abstractsection.component';
import { DefaultTypes } from '../../../../../shared/service/defaulttypes';
import { Utils } from '../../../../../shared/shared';

@Component({
    selector: '[storagesection]',
    templateUrl: './section.component.html'
})
export class StorageSectionComponent extends AbstractSection implements OnInit {

    private socValue: number

    constructor(translate: TranslateService) {
        super('Edge.Index.Energymonitor.Storage', "down", "#009846", translate);
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

    ngOnInit() {
        interval(1000)
            .subscribe(x => {
            })
    }

    public _updateCurrentData(sum: DefaultTypes.Summary): void {
        if (sum.storage.effectiveChargePower != null) {
            this.name = this.translate.instant('Edge.Index.Energymonitor.StorageCharge');
            super.updateSectionData(
                sum.storage.effectiveChargePower,
                sum.storage.powerRatio,
                Utils.divideSafely(sum.storage.effectiveChargePower, sum.system.totalPower));

        } else if (sum.storage.effectiveDischargePower != null) {
            this.name = this.translate.instant('Edge.Index.Energymonitor.StorageDischarge');
            super.updateSectionData(
                sum.storage.effectiveDischargePower,
                sum.storage.powerRatio,
                Utils.multiplySafely(
                    Utils.divideSafely(sum.storage.effectiveDischargePower, sum.system.totalPower), -1));

        } else {
            this.name = this.translate.instant('Edge.Index.Energymonitor.Storage')
            super.updateSectionData(null, null, null);
        }

        this.socValue = sum.storage.soc;
        if (sum.storage.soc) {
            this.valueText2 = Math.round(sum.storage.soc) + " %";
        } else {
            this.valueText2 = "";
        }

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
        return value + " W";
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "50%", y1: "0%", x2: "50%", y2: "100%" });
    }

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        let r = radius;
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