import { AbstractSection, SvgTextPosition, SvgImagePosition, SvgNumberPosition } from './abstractsection';

export class ProductionSection extends AbstractSection {
    constructor() {
        super("Erzeugung", "test", 316, 404, "#008DD2");
    }

    protected getTextPosition(outlineArc: any): SvgTextPosition {
        let centroid = outlineArc.centroid();
        return new SvgTextPosition(centroid[0], centroid[1] + 45, "middle");
    }

    protected getNumberPosition(outlineArc: any): SvgNumberPosition {
        let centroid = outlineArc.centroid();
        // console.log("PROD", centroid[0], centroid[1]);
        return new SvgNumberPosition(centroid[0], centroid[1] + 80, "middle");
    }

    protected getImagePosition(outlineArc: any): SvgImagePosition {
        let centroid = outlineArc.centroid();
        let height = this.height * 0.15;
        let y = (centroid[1] * 0.4687) * (-1);
        let x = y * 0.3684;
        // console.log("PROD", centroid[0], centroid[1]);
        return new SvgImagePosition("assets/img/production.png", centroid[0] - x, centroid[1] + y, height, height);
    }
}