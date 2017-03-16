import { AbstractSection, SvgRectPosition, SvgRect } from './abstractsection';

export class StorageSection extends AbstractSection {
    constructor() {
        super("Speicher", 136, 224, "#009846");
    }

    protected getRectPosition(rect: SvgRect, innerRadius: number): SvgRectPosition {
        let x = (rect.image.width / 2) * (-1);
        let y = innerRadius - 5 - (rect.image.y + rect.image.height);
        return new SvgRectPosition(x, y);
    }

    protected getImagePath(): string {
        return "storage.png";
    }

    protected getValueText(value: number): string {
        return value + " %";
    }
}