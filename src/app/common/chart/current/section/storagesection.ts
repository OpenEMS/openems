import { AbstractSection, SvgTextPosition } from './abstractsection';

export class StorageSection extends AbstractSection {
    constructor() {
        super("Speicher", 136, 224, "gray");
    }

    protected getTextPosition(arc: any): SvgTextPosition {
        let center = arc.centroid();
        return {
            x: center[0],
            y: center[1] - 30,
            anchor: "middle"
        }
    }
}