import { AbstractSection, SvgTextPosition } from './abstractsection';

export class ConsumptionSection extends AbstractSection {
    constructor() {
        super("Verbrauch", 46, 134, "gray");
    }

    protected getTextPosition(arc: any): SvgTextPosition {
        let center = arc.centroid();
        return {
            x: center[0] - 20,
            y: center[1],
            anchor: "end"
        }
    }
}