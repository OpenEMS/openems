import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from '../../../../../shared/service/defaulttypes';
import { Utils } from '../../../../../shared/shared';
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from './abstractsection.component';

@Component({
    selector: '[productionsection]',
    templateUrl: './section.component.html'
})
export class ProductionSectionComponent extends AbstractSection {

    constructor(translate: TranslateService) {
        super('General.Production', "up", "#008DD2", translate);
    }

    protected getStartAngle(): number {
        return 316;
    }

    protected getEndAngle(): number {
        return 404;
    }

    protected getRatioType(): Ratio {
        return 'Only Positive [0,1]';
    }

    protected _updateCurrentData(sum: DefaultTypes.Summary): void {
        super.updateSectionData(sum.production.activePower, sum.production.powerRatio, Utils.divideSafely(sum.production.activePower, sum.system.inPower));
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

    protected getSvgEnergyFlow(value: number, ratio: number, radius: number): SvgEnergyFlow {
        let p = {
            topLeft: { x: ratio * -1, y: radius * -1 },
            bottomLeft: { x: ratio * -1, y: ratio * -1 },
            topRight: { x: ratio, y: radius * -1 },
            bottomRight: { x: ratio, y: ratio * -1 },
            middleBottom: { x: 0, y: 0 },
            middleTop: { x: 0, y: radius * -1 + ratio }
        }
        if (value < 0) {
            // towards top
            p.topLeft.y = p.topLeft.y + radius;
            p.middleTop.y = p.middleTop.y - radius;
            p.topRight.y = p.topRight.y + radius;
        }
        return p;
    }
}