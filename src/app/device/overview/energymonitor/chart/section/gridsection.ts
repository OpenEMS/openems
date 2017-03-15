import { AbstractSection, SvgTextPosition, SvgImagePosition, SvgNumberPosition } from './abstractsection';

export class GridSection extends AbstractSection {
    constructor() {
        super("Netz", "test", 226, 314, "#1d1d1d");
    }

    protected getTextPosition(outlineArc: any): SvgTextPosition {
        let centroid = outlineArc.centroid();
        return new SvgTextPosition(centroid[0] + 50, centroid[1] - 50, "start");
    }

    protected getNumberPosition(outlineArc: any): SvgNumberPosition {
        let centroid = outlineArc.centroid();
        // console.log("GRID", centroid[0], centroid[1]);
        return new SvgNumberPosition(centroid[0] + 50, centroid[1] - 15, "start");
    }

    protected getImagePosition(outlineArc: any): SvgImagePosition {
        let centroid = outlineArc.centroid();
        let height = this.height * 0.15;
        let x = centroid[0] * 0.54;
        let y = x * 0.22;
        // console.log("GRID", centroid[0], centroid[1]);
        return new SvgImagePosition("assets/img/grid.png", centroid[0] + 35, centroid[1] + 10, height, height);
    }

    public setValue(value: number) {
        if (value > 50) {
            value = 50;
        } else if (value < -50) {
            value = 50;
        }
        this.value = value;
        this.update(this.innerRadius, this.outerRadius, this.height, this.width);
    }

    protected getValueStartAngle(): number {
        return (this.startAngle + this.endAngle) / 2;
    }
}