import * as d3 from 'd3';
import { DeviceOverviewEnergytableComponent } from '../../../energytable/energytable.component';
import { Device } from '../../../../../service/device';

export class SvgRectPosition {
    constructor(
        public x: number,
        public y: number
    ) { }
}

export class SvgRect {
    constructor(
        public valueRatio: SvgTextPosition,
        public valueText: SvgTextPosition,
        public image: SvgImagePosition
    ) { }
}

export class SvgTextPosition {
    constructor(
        public x: number,
        public y: number,
        public anchor: "start" | "middle" | "end",
        public fontsize: number
    ) { }
}

export class SvgImagePosition {
    constructor(
        public image: string,
        public x: number,
        public y: number,
        public width: number,
        public height: number
    ) { }
}

export abstract class AbstractSection {
    private outlinePath: number = null;
    private valuePath: number = null;
    protected valueRatio: number = 0;
    protected valueText: string = "";
    protected innerRadius: number = 0;
    protected outerRadius: number = 0;
    private textPosition: SvgTextPosition;
    private imagePosition: SvgImagePosition;
    private rect: SvgRect;
    private rectPosition: SvgRectPosition;
    protected height: number = 0;
    protected width: number = 0;

    constructor(
        private name: string,
        protected startAngle: number,
        protected endAngle: number,
        private color: string
    ) { }

    public update(outerRadius: number, innerRadius: number, height: number, width: number) {
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
        this.height = height;
        this.width = width;
        let outlineArc = this.getArc()
            .startAngle(this.deg2rad(this.startAngle))
            .endAngle(this.deg2rad(this.endAngle));
        // this.textPosition = this.getTextPosition(outlineArc);
        // this.imagePosition = this.getImagePosition(outlineArc);
        // this.numbäerPosition = this.getNumberPosition(outlineArc);
        this.rect = this.getRect(innerRadius);
        this.rectPosition = this.getRectPosition(this.rect, innerRadius);
        this.outlinePath = outlineArc();
        let valueEndAngle = ((this.endAngle - this.startAngle) * this.valueRatio) / 100 + this.getValueStartAngle();
        let valueArc = this.getArc()
            .startAngle(this.deg2rad(this.getValueStartAngle()))
            .endAngle(this.deg2rad(valueEndAngle));
        this.valuePath = valueArc();
    }

    protected getRect(innerRadius: any): SvgRect {
        let rWidth = innerRadius / 2.5;

        let textSize = rWidth / 4;
        let xText = rWidth / 2;
        let yText = textSize;

        let numberSize = textSize - 3;
        let xNumber = rWidth / 2;
        let yNumber = yText + 5 + numberSize;

        let imageSize = rWidth;
        let yImage = yNumber + 5;

        let rHeight = yImage + imageSize;

        return new SvgRect(
            new SvgTextPosition(xText, yText, "middle", textSize),
            new SvgTextPosition(xNumber, yNumber, "middle", numberSize),
            new SvgImagePosition("assets/img/" + this.getImagePath(), 0, yImage, rWidth, imageSize)
        );
    }

    protected abstract getImagePath(): string;
    protected abstract getRectPosition(rect: SvgRect, innerRadius: number): SvgRectPosition;
    // protected abstract getTextPosition(outlineArc: any): SvgTextPosition;
    // protected abstract getImagePosition(outlineArc: any): SvgImagePosition;
    // protected abstract getNumberPosition(outlineArc: any): SvgNumberPosition;
    protected abstract getValueText(value: number): string;

    protected getValueRatio(valueRatio: number): number {
        if (valueRatio > 100) {
            return 100;
        } else if (valueRatio < 0) {
            return 0;
        }
        return valueRatio;
    }

    public setValue(value: number, valueRatio: number) {
        this.valueRatio = this.getValueRatio(valueRatio);
        this.valueText = this.getValueText(value);
        this.update(this.innerRadius, this.outerRadius, this.height, this.width);
    }

    private getArc(): any {
        return d3.arc()
            .innerRadius(this.innerRadius)
            .outerRadius(this.outerRadius);
    }

    private deg2rad(value: number): number {
        return value * (Math.PI / 180)
    }

    protected getValueStartAngle(): number {
        return this.startAngle;
    }
}