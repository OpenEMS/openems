import { AbstractSection, SvgTextPosition, SvgImagePosition, SvgNumberPosition } from './abstractsection';

export class StorageSection extends AbstractSection {
    constructor() {
        super("Speicher", "test", 136, 224, "#009846");
    }

    protected getTextPosition(outlineArc: any): SvgTextPosition {
        let centroid = outlineArc.centroid();
        return new SvgTextPosition(centroid[0], centroid[1] - 125, "middle");
    }

    protected getNumberPosition(outlineArc: any): SvgNumberPosition {
        let centroid = outlineArc.centroid();
        // console.log("STOR", centroid[0], centroid[1]);
        return new SvgNumberPosition(centroid[0], centroid[1] - 90, "middle");
    }

    protected getImagePosition(outlineArc: any): SvgImagePosition {
        let centroid = outlineArc.centroid();
        let height = this.height * 0.15;
        let y = centroid[1] * 0.3947;
        let x = y * 0.4375;
        // console.log("STOR", centroid[0], centroid[1]);
        return new SvgImagePosition("assets/img/storage.png", centroid[0] - x, centroid[1] - y, height, height);
    }
}