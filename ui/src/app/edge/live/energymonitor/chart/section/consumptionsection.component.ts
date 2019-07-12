import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { WidgetClass } from 'src/app/shared/type/widget';
import { DefaultTypes } from '../../../../../shared/service/defaulttypes';
import { Service, Utils } from '../../../../../shared/shared';
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from './abstractsection.component';

@Component({
    selector: '[consumptionsection]',
    templateUrl: './section.component.html'
})
export class ConsumptionSectionComponent extends AbstractSection {

    constructor(
        translate: TranslateService,
        service: Service
    ) {
        super('General.Consumption', "right", "#FDC507", translate, service, "Consumption");
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
        super.updateSectionData(
            sum.consumption.activePower,
            sum.consumption.powerRatio,
            Utils.divideSafely(sum.consumption.activePower, sum.system.totalPower));
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

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        let r = radius;
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