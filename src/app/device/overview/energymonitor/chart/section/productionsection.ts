import { AbstractSection, SvgRectPosition, SvgRect } from './abstractsection';

export class ProductionSection extends AbstractSection {
    constructor() {
        super("Erzeugung", 316, 404, "#008DD2");
    }

    protected getRectPosition(rect: SvgRect, innerRadius: number): SvgRectPosition {
        let x = (rect.image.width / 2) * (-1);
        let y = (innerRadius - 5) * (-1);
        return new SvgRectPosition(x, y);
    }

    protected getImagePath(): string {
        return "production.png";
    }

    protected getValueText(value: number): string {
        return value + " W";
    }
}