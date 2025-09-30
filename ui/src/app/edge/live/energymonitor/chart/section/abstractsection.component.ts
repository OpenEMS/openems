// @ts-strict-ignore
import { TranslateService } from "@ngx-translate/core";
import * as d3 from "d3";
import { GridMode, Service } from "src/app/shared/shared";
import { DefaultTypes } from "../../../../../shared/type/defaulttypes";

export type Ratio = "Only Positive [0,1]" | "Negative and Positive [-1,1]";

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
    public state: "one" | "two" | "three" = "one";

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
            THIS.POINTS = "0,0 0,0";
        } else {
            const p = energyFlow;
            THIS.POINTS = P.TOP_LEFT.X + "," + P.TOP_LEFT.Y
                + (P.MIDDLE_TOP ? " " + P.MIDDLE_TOP.X + "," + P.MIDDLE_TOP.Y : "")
                + " " + P.TOP_RIGHT.X + "," + P.TOP_RIGHT.Y
                + (P.MIDDLE_RIGHT ? " " + P.MIDDLE_RIGHT.X + "," + P.MIDDLE_RIGHT.Y : "")
                + " " + P.BOTTOM_RIGHT.X + "," + P.BOTTOM_RIGHT.Y
                + (P.MIDDLE_BOTTOM ? " " + P.MIDDLE_BOTTOM.X + "," + P.MIDDLE_BOTTOM.Y : "")
                + " " + P.BOTTOM_LEFT.X + "," + P.BOTTOM_LEFT.Y
                + (P.MIDDLE_LEFT ? " " + P.MIDDLE_LEFT.X + "," + P.MIDDLE_LEFT.Y : "");
        }
        if (animationEnergyFlow == null) {
            THIS.ANIMATION_POINTS = "0,0 0,0";
        } else {
            const p = animationEnergyFlow;
            THIS.ANIMATION_POINTS = P.TOP_LEFT.X + "," + P.TOP_LEFT.Y
                + (P.MIDDLE_TOP ? " " + P.MIDDLE_TOP.X + "," + P.MIDDLE_TOP.Y : "")
                + " " + P.TOP_RIGHT.X + "," + P.TOP_RIGHT.Y
                + (P.MIDDLE_RIGHT ? " " + P.MIDDLE_RIGHT.X + "," + P.MIDDLE_RIGHT.Y : "")
                + " " + P.BOTTOM_RIGHT.X + "," + P.BOTTOM_RIGHT.Y
                + (P.MIDDLE_BOTTOM ? " " + P.MIDDLE_BOTTOM.X + "," + P.MIDDLE_BOTTOM.Y : "")
                + " " + P.BOTTOM_LEFT.X + "," + P.BOTTOM_LEFT.Y
                + (P.MIDDLE_LEFT ? " " + P.MIDDLE_LEFT.X + "," + P.MIDDLE_LEFT.Y : "");
        }
    }

    public switchState() {
        if (THIS.STATE == "one") {
            THIS.STATE = "two";
        } else if (THIS.STATE == "two") {
            THIS.STATE = "one";
        } else {
            THIS.STATE = "one";
        }
    }

    public hide() {
        THIS.STATE = "three";
    }
}

export abstract class AbstractSection {

    public fillRef: string = "";
    public valuePath: string = "";
    public outlinePath: string = "";
    public energyFlow: EnergyFlow | null = null;
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
    protected restrictionMode: number;

    private lastCurrentData: DEFAULT_TYPES.SUMMARY | null = null;

    constructor(
        translateName: string,
        protected direction: "left" | "right" | "down" | "up" = "left",
        public color: string,
        protected translate: TranslateService,
        protected service: Service,
        widgetClass: string,
    ) {
        THIS.SECTION_ID = translateName;
        THIS.NAME = TRANSLATE.INSTANT(translateName);
        THIS.ENERGY_FLOW = THIS.INIT_ENERGY_FLOW(0);
        SERVICE.GET_CONFIG().then(config => {
            CONFIG.WIDGETS.CLASSES.FOR_EACH(clazz => {
                if (CLAZZ.TO_STRING() === widgetClass) {
                    THIS.IS_ENABLED = true;
                }
            });
        });
    }

    /**
    * Updates the Values for this Section.
     *
     * @param sum the CURRENT_DATA.SUMMARY
    */
    public updateCurrentData(sum: DEFAULT_TYPES.SUMMARY): void {
        THIS.LAST_CURRENT_DATA = sum;
        this._updateCurrentData(sum);
    }

    /**
    * This method is called on every change of resolution of the browser window.
     */
    public updateOnWindowResize(outerRadius: number, innerRadius: number, height: number, width: number) {
        THIS.OUTER_RADIUS = outerRadius;
        THIS.INNER_RADIUS = innerRadius;
        THIS.HEIGHT = height;
        THIS.WIDTH = width;
        const outlineArc = THIS.GET_ARC()
            .startAngle(THIS.DEG2RAD(THIS.GET_START_ANGLE()))
            .endAngle(THIS.DEG2RAD(THIS.GET_END_ANGLE()));
        THIS.OUTLINE_PATH = outlineArc();

        /**
         * imaginary positioning "square"
         */
        THIS.SQUARE = THIS.GET_SQUARE(innerRadius);
        THIS.SQUARE_POSITION = THIS.GET_SQUARE_POSITION(THIS.SQUARE, innerRadius);
        /**
         * energy flow rectangle
         */
        const availableInnerRadius = innerRadius - THIS.SQUARE.IMAGE.Y - THIS.SQUARE.IMAGE.LENGTH - 10;
        THIS.ENERGY_FLOW = THIS.INIT_ENERGY_FLOW(availableInnerRadius);

        // now update also the value specific elements
        if (THIS.LAST_CURRENT_DATA) {
            THIS.UPDATE_CURRENT_DATA(THIS.LAST_CURRENT_DATA);
        }

        // update correct positioning for Image + Text
        THIS.SET_ELEMENT_HEIGHT();
    }

    /**
     * ATTR.FILL="{{ fillRef }}" has to be specific if using Safari (IOS Browser)
     * otherwise Energymonitor wont be displayed correctly
     */
    protected adjustFillRefbyBrowser(): void {
        if (NAVIGATOR.VENDOR.MATCH(/apple/i)) {
            THIS.FILL_REF = "url(" + WINDOW.LOCATION.ORIGIN + WINDOW.LOCATION.PATHNAME + "#" + THIS.SECTION_ID + ")";
        }
        else {
            THIS.FILL_REF = "url(#" + THIS.SECTION_ID + ")";
        }
    }

    protected getArc(): any {
        return D3.ARC()
            .innerRadius(THIS.INNER_RADIUS)
            .outerRadius(THIS.OUTER_RADIUS);
    }

    protected deg2rad(value: number): number {
        return value * (MATH.PI / 180);
    }

    /**
    * This method is called on every change of values.
    *
    * @param valueAbsolute the absolute value of the Section
    * @param valueRatio    the relative value of the Section in [-1,1]
    * @param sumRatio      the relative value of the Section compared to the total SYSTEM.IN_POWER/OutPower [0,1]
    */
    protected updateSectionData(valueAbsolute: number, valueRatio: number, sumRatio: number) {
        if (!THIS.IS_ENABLED) {
            return;
        }

        // TODO smoothly resize the arc
        THIS.VALUE_TEXT = THIS.GET_VALUE_TEXT(valueAbsolute);

        /*
         * Create the percentage Arc
         */
        let startAngle;
        switch (THIS.GET_RATIO_TYPE()) {
            case "Only Positive [0,1]":
                startAngle = THIS.GET_START_ANGLE();
                valueRatio = MATH.MIN(1, MATH.MAX(0, valueRatio));
                break;
            case "Negative and Positive [-1,1]":
                startAngle = (THIS.GET_START_ANGLE() + THIS.GET_END_ANGLE()) / 2;
                valueRatio = MATH.MIN(1, MATH.MAX(-1, valueRatio));
                break;
        }
        const valueEndAngle = (THIS.GET_END_ANGLE() - startAngle) * valueRatio + startAngle;
        const valueArc = THIS.GET_ARC()
            .startAngle(THIS.DEG2RAD(startAngle))
            .endAngle(THIS.DEG2RAD(valueEndAngle));
        THIS.VALUE_PATH = valueArc();

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
        const svgEnergyFlow = THIS.GET_SVG_ENERGY_FLOW(sumRatio, THIS.ENERGY_FLOW.RADIUS * 1.2);
        const svgAnimationEnergyFlow = THIS.GET_SVG_ANIMATION_ENERGY_FLOW(sumRatio, THIS.ENERGY_FLOW.RADIUS * 1.2);
        THIS.ENERGY_FLOW.UPDATE(svgEnergyFlow, svgAnimationEnergyFlow);
    }

    /**
     * calculate...
     * ...length of square and image;
     * ...x and y of text and image;
     * ...fontsize of text;
     */
    private getSquare(innerRadius: number): SvgSquare {
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
            new SvgImagePosition("assets/img/" + THIS.GET_IMAGE_PATH(), (length / 2) - (imageSize / 2), yImage, imageSize),
        );
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
     * Updates the Values for this Section. Should internally call updateSectionData().
     *
     * @param sum the CURRENT_DATA.SUMMARY
     */
    protected abstract _updateCurrentData(sum: DEFAULT_TYPES.SUMMARY): void;
    protected abstract getImagePath(): string;
    protected abstract getSquarePosition(rect: SvgSquare, innerRadius: number): SvgSquarePosition;
    protected abstract getValueText(value: number): string;
    protected abstract initEnergyFlow(radius: number): EnergyFlow;
    protected abstract setElementHeight();

}
