import { AbstractSection, SvgRectPosition, SvgRect } from './abstractsection';

export class ConsumptionSection extends AbstractSection {
    constructor() {
        super("Verbrauch", 46, 134, "#FDC507");
    }

    protected getRectPosition(rect: SvgRect, innerRadius: number): SvgRectPosition {
        let x = innerRadius - 5 - rect.image.width;
        let y = ((rect.image.y + rect.image.height) / 2) * (-1);
        return new SvgRectPosition(x, y);
    }

    protected getImagePath(): string {
        return "consumption.png";
    }

    protected getValueText(value: number): string {
        return value + " W";
    }
}