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
        if (sum.storage.chargeActivePower && sum.storage.chargeActivePower > 0) {
            this.name = this.translate.instant('Edge.Index.Energymonitor.StorageCharge');
            super.updateSectionData(sum.storage.chargeActivePower, sum.storage.powerRatio, Utils.divideSafely(sum.storage.chargeActivePower, sum.system.outPower));
        } else if (sum.storage.dischargeActivePower && sum.storage.dischargeActivePower > 0) {
            this.name = this.translate.instant('Edge.Index.Energymonitor.StorageCharge');
            super.updateSectionData(sum.storage.dischargeActivePower, sum.grid.powerRatio, Utils.divideSafely(sum.storage.dischargeActivePower, sum.system.inPower));
        } else {
            this.name = this.translate.instant('Edge.Index.Energymonitor.Storage')
            super.updateSectionData(0, 0, 0);
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
        } else if (this.socValue < 86) {
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

    protected getSvgEnergyFlow(value: number, ratio: number, radius: number): SvgEnergyFlow {
        let p = {
            topLeft: { x: ratio * -1, y: ratio },
            bottomLeft: { x: ratio * -1, y: radius },
            topRight: { x: ratio, y: ratio },
            bottomRight: { x: ratio, y: radius },
            middleBottom: { x: 0, y: radius - ratio },
            middleTop: { x: 0, y: 0 }
        }
        if (value > 0) {
            // towards bottom
            p.bottomLeft.y = p.bottomLeft.y - ratio;
            p.middleBottom.y = p.middleBottom.y + ratio;
            p.bottomRight.y = p.bottomRight.y - ratio;
        }
        return p;
    }
}