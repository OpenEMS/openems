import { AbstractSection, SvgTextPosition, SvgImagePosition } from './abstractsection';

export class StorageSection extends AbstractSection {
    constructor() {
        super("Speicher", 136, 224);
    }

    protected getTextPosition(outlineArc: any): SvgTextPosition {
        let centroid = outlineArc.centroid();
        return new SvgTextPosition(centroid[0], centroid[1] - 120, "middle");
    }

    protected getImagePosition(outlineArc: any): SvgImagePosition {
        let centroid = outlineArc.centroid();
        return new SvgImagePosition("/assets/img/storage.png", centroid[0] - 60, centroid[1] - 100)
    }
}