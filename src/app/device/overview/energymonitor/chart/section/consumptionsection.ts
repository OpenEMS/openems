import { AbstractSection, SvgTextPosition, SvgImagePosition } from './abstractsection';

export class ConsumptionSection extends AbstractSection {
    constructor() {
        super("Verbrauch", 46, 134);
    }

    protected getTextPosition(outlineArc: any): SvgTextPosition {
        let centroid = outlineArc.centroid();
        return new SvgTextPosition(centroid[0] - 30, centroid[1] - 30, "end");
    }

    protected getImagePosition(outlineArc: any): SvgImagePosition {
        let centroid = outlineArc.centroid();
        return new SvgImagePosition("/assets/img/consumption.png", centroid[0] - 130, centroid[1] - 10)
    }
}