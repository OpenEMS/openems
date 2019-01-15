import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from '../../../../../shared/service/defaulttypes';
import { Utils } from '../../../../../shared/shared';
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from './abstractsection.component';

@Component({
    selector: '[consumptionsection]',
    templateUrl: './section.component.html'
})
export class ConsumptionSectionComponent extends AbstractSection {

    constructor(translate: TranslateService) {
        super('General.Consumption', "right", "#FDC507", translate);
    }

    protected getStartAngle(): number {
        return 46;
    }

    protected getEndAngle(): number {
        return 134;
    }

    protected getRatioType(): Ratio {
        return 'Only Positive [0,1]';
    }

    protected _updateCurrentData(sum: DefaultTypes.Summary): void {
        super.updateSectionData(sum.consumption.activePower, sum.consumption.powerRatio, Utils.divideSafely(sum.consumption.activePower, sum.system.outPower));
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

    protected getSvgEnergyFlow(value: number, ratio: number, radius: number): SvgEnergyFlow {
        let p = {
            topLeft: { x: ratio, y: ratio * -1 },
            middleLeft: { x: 0, y: 0 },
            bottomLeft: { x: ratio, y: ratio },
            topRight: { x: radius, y: ratio * -1 },
            bottomRight: { x: radius, y: ratio },
            middleRight: { x: radius - ratio, y: 0 }
        }
        if (value > 0) {
            // towards right
            p.topRight.x = p.topRight.x - ratio;
            p.middleRight.x = p.middleRight.x + ratio;
            p.bottomRight.x = p.bottomRight.x - ratio;
        }
        return p;
    }
}