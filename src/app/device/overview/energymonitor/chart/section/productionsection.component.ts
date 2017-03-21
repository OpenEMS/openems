import { Component } from '@angular/core';
import { AbstractSectionComponent, SvgSquarePosition, SvgSquare, CircleDirection } from './abstractsection.component';

@Component({
    selector: '[app-device-overview-energymonitor-chart-productionsection]',
    templateUrl: './section.component.html'
})
export class ProductionSectionComponent extends AbstractSectionComponent {
    constructor() {
        super("Erzeugung", 316, 404, "#008DD2");
    }

    protected getCircleDirection(): CircleDirection {
        return new CircleDirection("up");
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
        return value + " W";
    }
}