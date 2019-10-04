import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from '../../../../../shared/service/defaulttypes';
import { Service, Utils } from '../../../../../shared/shared';
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from './abstractsection.component';
import { UnitvaluePipe } from 'src/app/shared/pipe/unitvalue/unitvalue.pipe';
import { trigger, state, style, animate, transition } from '@angular/animations';


@Component({
    selector: '[consumptionsection]',
    templateUrl: './section.component.html',
    animations: [
        trigger('colored', [
            state('true', style({
                height: '200px',
                opacity: 1,
                backgroundColor: 'yellow'
            })),
            state('false', style({
                height: '100px',
                opacity: 0.5,
                backgroundColor: 'green'
            })),
            transition('false <=> true', animate('1s'))
        ])
    ]
})
export class ConsumptionSectionComponent extends AbstractSection {

    private unitpipe: UnitvaluePipe;
    public colored = true;


    constructor(
        unitpipe: UnitvaluePipe,
        translate: TranslateService,
        service: Service,
    ) {
        super('General.Consumption', "right", "#FDC507", translate, service, "Consumption");
        this.unitpipe = unitpipe;
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
        return this.unitpipe.transform(value, 'kW')
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