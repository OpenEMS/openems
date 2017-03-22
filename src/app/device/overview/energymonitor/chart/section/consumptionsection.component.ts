import { Component } from '@angular/core';
import { AbstractSectionComponent, SvgSquarePosition, SvgSquare, CircleDirection } from './abstractsection.component';

@Component({
    selector: '[consumptionsection]',
    templateUrl: './section.component.html'
})
export class ConsumptionSectionComponent extends AbstractSectionComponent {
    constructor() {
        super("Verbrauch", 46, 134, "#FDC507");
    }

    protected getCircleDirection(): CircleDirection {
        return new CircleDirection("right");
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
        return value + " W";
    }
}