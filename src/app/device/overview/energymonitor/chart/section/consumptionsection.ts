import { AbstractSection, SvgSquarePosition, SvgSquare, CircleDirection } from './abstractsection';

export class ConsumptionSection extends AbstractSection {
    constructor() {
        super("Verbrauch", 46, 134, "#FDC507");
    }

    protected getCircleDirection(): CircleDirection {
        return new CircleDirection("right");
    }

    protected getRectPosition(rect: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = innerRadius - 5 - rect.length;
        let y = (rect.length / 2) * (-1);
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        return "consumption.png";
    }

    protected getValueText(value: number): string {
        return value + " W";
    }
}