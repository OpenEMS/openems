import { AbstractSection, SvgTextPosition } from './abstractsection';

export class GridSection extends AbstractSection {
    constructor() {
        super("Netz", 226, 314);
    }

    protected getTextPosition(arc: any): SvgTextPosition {
        let center = arc.centroid();
        return {
            x: center[0] + 20,
            y: center[1],
            anchor: "start"
        }
    }
}