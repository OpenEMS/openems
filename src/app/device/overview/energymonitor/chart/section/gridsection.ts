import { AbstractSection, SvgSquarePosition, SvgSquare, CircleDirection } from './abstractsection';

export class GridSection extends AbstractSection {
    constructor() {
        super("Netz", 226, 314, "#1d1d1d");
    }

    protected getCircleDirection(): CircleDirection {
        return new CircleDirection("left");
    }

    protected getSquarePosition(square: SvgSquare, innerRadius: number): SvgSquarePosition {
        let x = (innerRadius - 5) * (-1);
        let y = (square.length / 2) * (-1);
        return new SvgSquarePosition(x, y);
    }

    protected getImagePath(): string {
        return "grid.png";
    }

    public getValueRatio(value: number) {
        if (value > 50) {
            return 50;
        } else if (value < -50) {
            return 50;
        }
        return value;
    }

    protected getValueStartAngle(): number {
        return (this.startAngle + this.endAngle) / 2;
    }

    protected getValueText(value: number): string {
        return value + " W";
    }
}