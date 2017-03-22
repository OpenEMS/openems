import { Component } from '@angular/core';
import { AbstractSectionComponent, SvgSquarePosition, SvgSquare, CircleDirection } from './abstractsection.component';

@Component({
    selector: '[storagesection]',
    templateUrl: './section.component.html'
})
export class StorageSectionComponent extends AbstractSectionComponent {
    constructor() {
        super("Speicher", 136, 224, "#009846");
    }

    protected getCircleDirection(): CircleDirection {
        return new CircleDirection("down");
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = (square.length / 2) * (-1);
        let y = innerRadius - 5 - square.length;
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        return "storage.png";
    }

    protected getValueText(value: number): string {
        return value + " %";
    }
}