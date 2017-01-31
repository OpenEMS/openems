import { AbstractSection, SvgTextPosition } from './abstractsection';

export class ProductionSection extends AbstractSection {
    constructor() {
        super("Erzeugung", 316, 404);
    }

    protected getTextPosition(arc: any): SvgTextPosition {
        let center = arc.centroid();
        return {
            x: center[0],
            y: center[1] + 40,
            anchor: "middle"
        }
    }
}