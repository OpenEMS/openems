import { AbstractSection, SvgTextPosition, SvgImagePosition } from './abstractsection';

export class ProductionSection extends AbstractSection {
    constructor() {
        super("Erzeugung", 316, 404);
    }

    protected getTextPosition(outlineArc: any): SvgTextPosition {
        let centroid = outlineArc.centroid();
        return new SvgTextPosition(centroid[0], centroid[1] + 45, "middle");
    }

    protected getImagePosition(outlineArc: any): SvgImagePosition {
        let centroid = outlineArc.centroid();
        return new SvgImagePosition("/assets/img/production.png", centroid[0] - 60, centroid[1] + 70)
    }
}