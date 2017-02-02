import { AbstractSection, SvgTextPosition, SvgImagePosition } from './abstractsection';

export class GridSection extends AbstractSection {
    constructor() {
        super("Netz", 226, 314);
    }

    protected getTextPosition(outlineArc: any): SvgTextPosition {
        let centroid = outlineArc.centroid();
        return new SvgTextPosition(centroid[0] + 50, centroid[1] - 30, "start");
    }

    protected getImagePosition(outlineArc: any): SvgImagePosition {
        let centroid = outlineArc.centroid();
        return new SvgImagePosition("/assets/img/grid.png", centroid[0] + 10, centroid[1] - 10)
    }
}