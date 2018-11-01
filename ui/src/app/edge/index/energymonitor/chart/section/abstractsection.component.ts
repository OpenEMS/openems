import { TranslateService } from '@ngx-translate/core';
import * as d3 from 'd3';

export class SectionValue {
    absolute: number;
    ratio: number;
}

export class SvgSquarePosition {
    constructor(
        public x: number,
        public y: number
    ) { }
}

export class SvgSquare {
    constructor(
        public length: number,
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
        public length: number
    ) { }
}

export interface SvgEnergyFlow {
    topLeft: { x: number, y: number },
    middleLeft?: { x: number, y: number },
    bottomLeft: { x: number, y: number },
    middleBottom?: { x: number, y: number },
    bottomRight: { x: number, y: number },
    middleRight?: { x: number, y: number },
    topRight: { x: number, y: number },
    middleTop?: { x: number, y: number }
}

export class EnergyFlow {
    public points: string = "0,0 0,0";

    constructor(
        public radius: number,
        public gradient: {
            x1: string,
            y1: string,
            x2: string,
            y2: string
        }
    ) { }

    public update(p: SvgEnergyFlow) {
        if (p == null) {
            this.points = "0,0 0,0";
        } else {
            this.points = p.topLeft.x + "," + p.topLeft.y
                + (p.middleTop ? " " + p.middleTop.x + "," + p.middleTop.y : "")
                + " " + p.topRight.x + "," + p.topRight.y
                + (p.middleRight ? " " + p.middleRight.x + "," + p.middleRight.y : "")
                + " " + p.bottomRight.x + "," + p.bottomRight.y
                + (p.middleBottom ? " " + p.middleBottom.x + "," + p.middleBottom.y : "")
                + " " + p.bottomLeft.x + "," + p.bottomLeft.y
                + (p.middleLeft ? " " + p.middleLeft.x + "," + p.middleLeft.y : "");
        }
    }

    public state: "one" | "two" | "three" = "one";

    public switchState() {
        if (this.state == 'one') {
            this.state = 'two';
        } else if (this.state == 'two') {
            this.state = 'one';
        } else {
            this.state = 'one';
        }
    }

    public hide() {
        this.state = 'three';
    }
}

export abstract class AbstractSection {

    public url: string = window.location.href;
    public valuePath: string = "";
    public outlinePath: string = "";
    public energyFlow: EnergyFlow;
    public square: SvgSquare;
    public squarePosition: SvgSquarePosition;
    public name: string = "";
    public sectionId: string = "";

    protected valueRatio: number = 0;
    protected valueText: string = "";
    protected valueText2: string = "";
    protected innerRadius: number = 0;
    protected outerRadius: number = 0;
    protected height: number = 0;
    protected width: number = 0;
    protected lastValue = { valueAbsolute: 0, valueRatio: 0, sumRatio: 0 };

    constructor(
        translateName: string,
        protected direction: "left" | "right" | "down" | "up" = "left",
        protected startAngle: number,
        protected endAngle: number,
        public color: string,
        protected translate: TranslateService
    ) {
        this.sectionId = translateName;
        this.name = translate.instant(translateName);
        this.energyFlow = this.initEnergyFlow(0);
    }

    /**
     * This method is called on every change of values.
     */
    protected updateValue(valueAbsolute: number, valueRatio: number, sumRatio: number) {
        // TODO smoothly resize the arc
        this.lastValue = { valueAbsolute: valueAbsolute, valueRatio: valueRatio, sumRatio: sumRatio };
        this.valueRatio = this.getValueRatio(valueRatio);
        this.valueText = this.getValueText(valueAbsolute);
        let valueEndAngle = ((this.endAngle - this.startAngle) * this.valueRatio) / 100 + this.getValueStartAngle();
        let valueArc = this.getArc()
            .startAngle(this.deg2rad(this.getValueStartAngle()))
            .endAngle(this.deg2rad(valueEndAngle));
        this.valuePath = valueArc();

        let energyFlowValue = Math.abs(Math.round(sumRatio * 10));
        if (energyFlowValue < -10) {
            energyFlowValue = -10;
        } else if (energyFlowValue > 10) {
            energyFlowValue = 10;
        }
        let svgEnergyFlow;
        if (isNaN(sumRatio) || isNaN(energyFlowValue)) {
            svgEnergyFlow = null;
        } else {
            svgEnergyFlow = this.getSvgEnergyFlow(sumRatio, this.energyFlow.radius, energyFlowValue);
        }
        this.energyFlow.update(svgEnergyFlow);
    }

    /**
     * This method is called on every change of resolution of the browser window.
     */
    public update(outerRadius: number, innerRadius: number, height: number, width: number) {
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
        this.height = height;
        this.width = width;
        let outlineArc = this.getArc()
            .startAngle(this.deg2rad(this.startAngle))
            .endAngle(this.deg2rad(this.endAngle));
        this.outlinePath = outlineArc();

        /**
         * imaginary positioning "square"
         */
        this.square = this.getSquare(innerRadius);
        this.squarePosition = this.getSquarePosition(this.square, innerRadius);

        /**
         * energy flow rectangle
         */
        let availableInnerRadius = innerRadius - this.square.image.y - this.square.image.length - 10;
        this.energyFlow = this.initEnergyFlow(availableInnerRadius);

        // now update also the value specific elements
        this.updateValue(this.lastValue.valueAbsolute, this.lastValue.valueRatio, this.lastValue.sumRatio);
    }

    /**
     * calculate...
     * ...length of square and image;
     * ...x and y of text and image;
     * ...fontsize of text;
     *
     */
    protected getSquare(innerRadius: any): SvgSquare {
        let width = innerRadius / 2.5;

        let textSize = width / 4;
        let yText = textSize;

        let numberSize = textSize - 3;
        let yNumber = yText + 5 + numberSize;

        let imageSize = width;
        let yImage = yNumber + 5;

        let length = yImage + imageSize;

        let xText = length / 2;

        return new SvgSquare(
            length,
            new SvgTextPosition(xText, yText, "middle", textSize),
            new SvgTextPosition(xText, yNumber, "middle", numberSize),
            new SvgImagePosition("assets/img/" + this.getImagePath(), (length / 2) - (imageSize / 2), yImage, imageSize)
        );
    }

    protected abstract getImagePath(): string;
    protected abstract getSquarePosition(rect: SvgSquare, innerRadius: number): SvgSquarePosition;
    protected abstract getValueText(value: number): string;
    protected abstract initEnergyFlow(radius: number): EnergyFlow;
    // v is between -10 and 10
    protected abstract getSvgEnergyFlow(ratio: number, r: number, v: number): SvgEnergyFlow;

    protected getValueRatio(valueRatio: number): number {
        if (valueRatio > 100) {
            return 100;
        } else if (valueRatio < 0) {
            return 0;
        } else if (valueRatio == null || Number.isNaN(valueRatio)) {
            return 0;
        }
        return valueRatio;
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