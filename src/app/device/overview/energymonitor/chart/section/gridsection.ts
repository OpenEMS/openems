import { AbstractSection, SvgRectPosition, SvgRect } from './abstractsection';

export class GridSection extends AbstractSection {
    constructor() {
        super("Netz", 226, 314, "#1d1d1d");
    }

    protected getRectPosition(rect: SvgRect, innerRadius: number): SvgRectPosition {
        let x = (innerRadius - 5) * (-1);
        let y = ((rect.image.y + rect.image.height) / 2) * (-1);
        return new SvgRectPosition(x, y);
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