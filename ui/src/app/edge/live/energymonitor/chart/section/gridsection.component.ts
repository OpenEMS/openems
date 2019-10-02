import { Component } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { DefaultTypes } from '../../../../../shared/service/defaulttypes';
import { Service, Utils } from '../../../../../shared/shared';
import { AbstractSection, EnergyFlow, Ratio, SvgEnergyFlow, SvgSquare, SvgSquarePosition } from './abstractsection.component';
import { WidgetClass } from 'src/app/shared/type/widget';
import { UnitvaluePipe } from 'src/app/shared/pipe/unitvalue/unitvalue.pipe';

@Component({
    selector: '[gridsection]',
    templateUrl: './section.component.html'
})
export class GridSectionComponent extends AbstractSection {

    private unitpipe: UnitvaluePipe;

    constructor(
        translate: TranslateService,
        service: Service,
        unitpipe: UnitvaluePipe,
    ) {
        super('General.Grid', "left", "#1d1d1d", translate, service, "Grid");
        this.unitpipe = unitpipe;
    }

    protected getStartAngle(): number {
        return 226;
    }

    protected getEndAngle(): number {
        return 314;
    }

    protected getRatioType(): Ratio {
        return 'Negative and Positive [-1,1]';
    }

    public _updateCurrentData(sum: DefaultTypes.Summary): void {
        if (sum.grid.buyActivePower && sum.grid.buyActivePower > 0) {
            this.name = this.translate.instant('General.GridBuy');
            super.updateSectionData(
                sum.grid.buyActivePower,
                sum.grid.powerRatio,
                Utils.multiplySafely(
                    Utils.divideSafely(sum.grid.buyActivePower, sum.system.totalPower), -1));
        } else if (sum.grid.sellActivePower && sum.grid.sellActivePower > 0) {
            this.name = this.translate.instant('General.GridSell');
            super.updateSectionData(
                sum.grid.sellActivePower,
                sum.grid.powerRatio,
                Utils.divideSafely(sum.grid.sellActivePower, sum.system.totalPower));
        } else {
            this.name = this.translate.instant('General.Grid')
            super.updateSectionData(null, null, null);
        }

        // set grid mode
        this.gridMode = sum.grid.gridMode;
        if (this.square) {
            this.square.image.image = "assets/img/" + this.getImagePath()
        }
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = (innerRadius - 5) * (-1);
        let y = (square.length / 2) * (-1);
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        if (this.gridMode == 0 || this.gridMode == 2) {
            return "offgrid.png"
        } else {
            return "grid.png"
        }
    }

    protected getValueText(value: number): string {
        if (value == null || Number.isNaN(value)) {
            return "";
        }

        return this.unitpipe.transform(value, 'kW');
    }

    protected initEnergyFlow(radius: number): EnergyFlow {
        return new EnergyFlow(radius, { x1: "100%", y1: "50%", x2: "0%", y2: "50%" });
    }

    protected getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow {
        let v = Math.abs(ratio);
        let r = radius;
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
