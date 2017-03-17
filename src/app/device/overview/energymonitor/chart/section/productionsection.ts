import { AbstractSection, SvgSquarePosition, SvgSquare, CircleDirection } from './abstractsection';

export class ProductionSection extends AbstractSection {
    constructor() {
        super("Erzeugung", 316, 404, "#008DD2");
    }

    protected getCircleDirection(): CircleDirection {
        return new CircleDirection("up");
    }

    protected getRectPosition(rect: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = (rect.length / 2) * (-1);
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