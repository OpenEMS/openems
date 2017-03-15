import { AbstractSection, SvgTextPosition, SvgImagePosition, SvgNumberPosition } from './abstractsection';

export class ConsumptionSection extends AbstractSection {
    constructor() {
        super("Verbrauch", "test", 46, 134, "#FDC507");
    }

    protected getTextPosition(outlineArc: any): SvgTextPosition {
        let centroid = outlineArc.centroid();
        return new SvgTextPosition(centroid[0] - 30, centroid[1] - 50, "end");
    }

    protected getNumberPosition(outlineArc: any): SvgNumberPosition {
        let centroid = outlineArc.centroid();
        // console.log("CONS", centroid[0], centroid[1]);
        return new SvgNumberPosition(centroid[0] - 60, centroid[1] - 15, "end");
    }

    protected getImagePosition(outlineArc: any): SvgImagePosition {
        let centroid = outlineArc.centroid();
        let height = this.height * 0.15;
        let x = centroid[0] * 0.54;
        let y = x * 0.22;
        // console.log("CONS", centroid[0], centroid[1]);
        return new SvgImagePosition("assets/img/consumption.png", centroid[0] - 110, centroid[1] + 10, height, height);
    }
}