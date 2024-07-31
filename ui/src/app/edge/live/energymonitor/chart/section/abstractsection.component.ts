// @ts-strict-ignore
import { DefaultTypes } from '../../../../../shared/service/defaulttypes';
import { Service } from 'src/app/shared/shared';
import { TranslateService } from '@ngx-translate/core';
import * as d3 from 'd3';

export type Ratio = 'Only Positive [0,1]' | 'Negative and Positive [-1,1]';

export class SectionValue {
    public absolute: number;
    public ratio: number;
}

export class SvgSquarePosition {
    constructor(
        public x: number,
        public y: number,
    ) { }
}

export class SvgSquare {
    constructor(
        public length: number,
        public valueRatio: SvgTextPosition,
        public valueText: SvgTextPosition,
        public image: SvgImagePosition,
    ) { }
}

export class SvgTextPosition {
    constructor(
        public x: number,
        public y: number,
        public anchor: "start" | "middle" | "end",
        public fontsize: number,
    ) { }
}

export class SvgImagePosition {
    constructor(
        public image: string,
        public x: number,
        public y: number,
        public length: number,
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
    public animationPoints: string = "0,0 0,0";

    constructor(
        public radius: number,
        public gradient: {
            x1: string,
            y1: string,
            x2: string,
            y2: string
        },
    ) { }

    public update(energyFlow: SvgEnergyFlow, animationEnergyFlow: SvgEnergyFlow) {
        if (energyFlow == null) {
            this.points = "0,0 0,0";
        } else {
            const p = energyFlow;
            this.points = p.topLeft.x + "," + p.topLeft.y
                + (p.middleTop ? " " + p.middleTop.x + "," + p.middleTop.y : "")
                + " " + p.topRight.x + "," + p.topRight.y
                + (p.middleRight ? " " + p.middleRight.x + "," + p.middleRight.y : "")
                + " " + p.bottomRight.x + "," + p.bottomRight.y
                + (p.middleBottom ? " " + p.middleBottom.x + "," + p.middleBottom.y : "")
                + " " + p.bottomLeft.x + "," + p.bottomLeft.y
                + (p.middleLeft ? " " + p.middleLeft.x + "," + p.middleLeft.y : "");
        }
        if (animationEnergyFlow == null) {
            this.animationPoints = "0,0 0,0";
        } else {
            const p = animationEnergyFlow;
            this.animationPoints = p.topLeft.x + "," + p.topLeft.y
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

export enum GridMode {
    "undefined",
    "ongrid",
    "offgrid",
}

export abstract class AbstractSection {

    public fillRef: string = "";
    public valuePath: string = "";
    public outlinePath: string = "";
    public energyFlow: EnergyFlow = null;
    public square: SvgSquare;
    public squarePosition: SvgSquarePosition;
    public name: string = "";
    public sectionId: string = "";
    public isEnabled: boolean = false;
    public animationSpeed: number = 500;

    protected valueText: string = "";
    protected innerRadius: number = 0;
    protected outerRadius: number = 0;
    protected height: number = 0;
    protected width: number = 0;
    protected gridMode: GridMode;

    private lastCurrentData: DefaultTypes.Summary = null;

    constructor(
        translateName: string,
        protected direction: "left" | "right" | "down" | "up" = "left",
        public color: string,
        protected translate: TranslateService,
        protected service: Service,
        widgetClass: string,
    ) {
        this.sectionId = translateName;
        this.name = translate.instant(translateName);
        this.energyFlow = this.initEnergyFlow(0);
        service.getConfig().then(config => {
            config.widgets.classes.forEach(clazz => {
                if (clazz.toString() === widgetClass) {
                    this.isEnabled = true;
                }
            });
        });
    }

    /**
     * attr.fill="{{ fillRef }}" has to be specific if using Safari (IOS Browser)
     * otherwise Energymonitor wont be displayed correctly
     */
    protected adjustFillRefbyBrowser(): void {
        if (navigator.vendor.match(/apple/i)) {
            this.fillRef = "url(" + window.location.origin + window.location.pathname + "#" + this.sectionId + ")";
        }
        else {
            this.fillRef = "url(#" + this.sectionId + ")";
        }
    }

    /**
     * Gets the Start-Angle in Degree
     */
    protected abstract getStartAngle(): number;

    /**
     * Gets the End-Angle in Degree
     */
    protected abstract getEndAngle(): number;

    /**
     * Gets the Ratio-Type of this Section
     */
    protected abstract getRatioType(): Ratio;

    /**
     * Gets the SVG for EnergyFlow
     *
     * @param ratio  the ratio of the value [-1,1] * scale factor
     * @param radius the available radius
     */
    protected abstract getSvgEnergyFlow(ratio: number, radius: number): SvgEnergyFlow;

    /**
     * Gets the SVG for EnergyFlowAnimation
     *
     * @param ratio  the ratio of the value [-1,1] * scale factor
     * @param radius the available radius
     */
    protected abstract getSvgAnimationEnergyFlow(ratio: number, radius: number): SvgEnergyFlow;

    /**
     * Updates the Values for this Section.
     *
     * @param sum the CurrentData.Summary
     */
    public updateCurrentData(sum: DefaultTypes.Summary): void {
        this.lastCurrentData = sum;
        this._updateCurrentData(sum);
    }

    /**
     * Updates the Values for this Section. Should internally call updateSectionData().
     *
     * @param sum the CurrentData.Summary
     */
    protected abstract _updateCurrentData(sum: DefaultTypes.Summary): void;

    /**
     * This method is called on every change of values.
     *
     * @param valueAbsolute the absolute value of the Section
     * @param valueRatio    the relative value of the Section in [-1,1]
     * @param sumRatio      the relative value of the Section compared to the total System.InPower/OutPower [0,1]
     */
    protected updateSectionData(valueAbsolute: number, valueRatio: number, sumRatio: number) {
        if (!this.isEnabled) {
            return;
        }

        // TODO smoothly resize the arc
        this.valueText = this.getValueText(valueAbsolute);

        /*
         * Create the percentage Arc
         */
        let startAngle;
        switch (this.getRatioType()) {
            case "Only Positive [0,1]":
                startAngle = this.getStartAngle();
                valueRatio = Math.min(1, Math.max(0, valueRatio));
                break;
            case "Negative and Positive [-1,1]":
                startAngle = (this.getStartAngle() + this.getEndAngle()) / 2;
                valueRatio = Math.min(1, Math.max(-1, valueRatio));
                break;
        }
        const valueEndAngle = (this.getEndAngle() - startAngle) * valueRatio + startAngle;
        const valueArc = this.getArc()
            .startAngle(this.deg2rad(startAngle))
            .endAngle(this.deg2rad(valueEndAngle));
        this.valuePath = valueArc();

        /*
         * Create the energy flow direction arrow
         */
        if (!sumRatio) {
            sumRatio = 0;
        } else if (sumRatio > 0 && sumRatio < 0.1) {
            sumRatio = 0.1; // scale ratio to [0.1,1]
        } else if (sumRatio < 0 && sumRatio > -0.1) {
            sumRatio = -0.1; // scale ratio to [-0.1,-1]
        }
        sumRatio *= 10;

        //radius * 1.2 for longer arrows
        const svgEnergyFlow = this.getSvgEnergyFlow(sumRatio, this.energyFlow.radius * 1.2);
        const svgAnimationEnergyFlow = this.getSvgAnimationEnergyFlow(sumRatio, this.energyFlow.radius * 1.2);
        this.energyFlow.update(svgEnergyFlow, svgAnimationEnergyFlow);
    }

    /**
     * This method is called on every change of resolution of the browser window.
     */
    public updateOnWindowResize(outerRadius: number, innerRadius: number, height: number, width: number) {
        this.outerRadius = outerRadius;
        this.innerRadius = innerRadius;
        this.height = height;
        this.width = width;
        const outlineArc = this.getArc()
            .startAngle(this.deg2rad(this.getStartAngle()))
            .endAngle(this.deg2rad(this.getEndAngle()));
        this.outlinePath = outlineArc();

        /**
         * imaginary positioning "square"
         */
        this.square = this.getSquare(innerRadius);
        this.squarePosition = this.getSquarePosition(this.square, innerRadius);
        /**
         * energy flow rectangle
         */
        const availableInnerRadius = innerRadius - this.square.image.y - this.square.image.length - 10;
        this.energyFlow = this.initEnergyFlow(availableInnerRadius);

        // now update also the value specific elements
        if (this.lastCurrentData) {
            this.updateCurrentData(this.lastCurrentData);
        }

        // update correct positioning for Image + Text
        this.setElementHeight();
    }

    /**
     * calculate...
     * ...length of square and image;
     * ...x and y of text and image;
     * ...fontsize of text;
     */
    private getSquare(innerRadius: any): SvgSquare {
        const width = innerRadius / 2.5;

        const textSize = width / 4;
        const yText = textSize;

        const numberSize = textSize - 3;
        const yNumber = yText + 5 + numberSize;

        const imageSize = width;
        const yImage = yNumber + 5;

        const length = yImage + imageSize;

        const xText = length / 2;

        return new SvgSquare(
            length,
            new SvgTextPosition(xText, yText, "middle", textSize),
            new SvgTextPosition(xText, yNumber, "middle", numberSize),
            new SvgImagePosition("assets/img/" + this.getImagePath(), (length / 2) - (imageSize / 2), yImage, imageSize),
        );
    }

    protected abstract getImagePath(): string;
    protected abstract getSquarePosition(rect: SvgSquare, innerRadius: number): SvgSquarePosition;
    protected abstract getValueText(value: number): string;
    protected abstract initEnergyFlow(radius: number): EnergyFlow;
    protected abstract setElementHeight();

    protected getArc(): any {
        return d3.arc()
            .innerRadius(this.innerRadius)
            .outerRadius(this.outerRadius);
    }

    protected deg2rad(value: number): number {
        return value * (Math.PI / 180);
    }

}
