import * as d3 from 'd3';

export interface SvgTextPosition {
    x: number,
    y: number
    anchor: "start" | "middle" | "end"
}

export abstract class AbstractSection {
    private outlinePath: number = null;
    private path: number = null;
    private value: number = 10;
    private innerRadius: number = 0;
    private outerRadius: number = 0;
    private textPosition = null;
    private color: string = "gray";

    constructor(
        private name: string,
        private startAngle: number,
        private endAngle: number
    ) { }

    public update(outerRadius: number, innerRadius: number) {
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
        let arc = this.getArc();
        let outlineArc = arc.endAngle(this.deg2rad(this.endAngle))
        this.textPosition = this.getTextPosition(outlineArc);
        this.outlinePath = outlineArc();
        let valueEndAngle = ((this.endAngle - this.startAngle) * this.value) / 100 + this.startAngle;
        this.path = arc.endAngle(this.deg2rad(valueEndAngle))();
    }

    protected abstract getTextPosition(outlineArc: any): SvgTextPosition;

    public setValue(value: number) {
        if (value > 100) {
            value = 100;
        } else if (value < 0) {
            value = 0;
        }
        this.value = value;
        this.update(this.innerRadius, this.outerRadius);
    }

    public getValue(): number {
        return this.value;
    }

    private getArc(): any {
        return d3.arc()
            .innerRadius(this.innerRadius)
            .outerRadius(this.outerRadius)
            .startAngle(this.deg2rad(this.startAngle));
    }

    private deg2rad(value: number): number {
        return value * (Math.PI / 180)
    }
}