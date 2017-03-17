import { AbstractSection, SvgSquarePosition, SvgSquare, CircleDirection } from './abstractsection';

export class ConsumptionSection extends AbstractSection {
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