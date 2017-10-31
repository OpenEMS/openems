import { Component, Input, trigger, state, style, transition, animate } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import * as d3 from 'd3';

import { EnergytableComponent } from '../../../energytable/energytable.component';
import { Device } from '../../../../../shared/device/device';

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

export class EnergyFlow {
    public points: string = "";

    constructor(
        private direction: "left" | "right" | "down" | "up",
        private radius: number,
    ) {

    }

    public update(ratio: number) {
        if (ratio == 0) {
            this.points = "";
        } else {
            let r = this.radius;
            let v = Math.abs(Math.round(ratio * 10));
            let p: {
                topLeft: { x: number, y: number },
                middleLeft?: { x: number, y: number },
                bottomLeft: { x: number, y: number },
                middleBottom?: { x: number, y: number },
                bottomRight: { x: number, y: number },
                middleRight?: { x: number, y: number },
                topRight: { x: number, y: number },
                middleTop?: { x: number, y: number }
            };
            if (this.direction == "left") {
                p = {
                    topLeft: { x: r * -1, y: v * -1 },
                    middleLeft: { x: r * -1 + v, y: 0 },
                    bottomLeft: { x: r * -1, y: v },
                    topRight: { x: 0, y: v * -1 },
                    bottomRight: { x: 0, y: v },
                    middleRight: { x: 0, y: 0 }
                }
                if (ratio > 0) {
                    // towards left
                    p.topLeft.x = p.topLeft.x + v;
                    p.middleLeft.x = p.middleLeft.x - v;
                    p.bottomLeft.x = p.bottomLeft.x + v;
                }
            } else if (this.direction == "right") {
                p = {
                    topLeft: { x: v, y: v * -1 },
                    middleLeft: { x: 0, y: 0 },
                    bottomLeft: { x: v, y: v },
                    topRight: { x: r, y: v * -1 },
                    bottomRight: { x: r, y: v },
                    middleRight: { x: r - v, y: 0 }
                }
                if (ratio > 0) {
                    // towards right
                    p.topRight.x = p.topRight.x - v;
                    p.middleRight.x = p.middleRight.x + v;
                    p.bottomRight.x = p.bottomRight.x - v;
                }
            } else if (this.direction == "up") {
                p = {
                    topLeft: { x: v * -1, y: r * -1 },
                    bottomLeft: { x: v * -1, y: v * -1 },
                    topRight: { x: v, y: r * -1 },
                    bottomRight: { x: v, y: v * -1 },
                    middleBottom: { x: 0, y: 0 },
                    middleTop: { x: 0, y: r * -1 + v }
                }
                if (ratio > 0) {
                    // towards top
                    p.topLeft.y = p.topLeft.y + v;
                    p.middleTop.y = p.middleTop.y - v;
                    p.topRight.y = p.topRight.y + v;
                }
            } else if (this.direction == "down") {
                p = {
                    topLeft: { x: v * -1, y: v },
                    bottomLeft: { x: v * -1, y: r },
                    topRight: { x: v, y: v },
                    bottomRight: { x: v, y: r },
                    middleBottom: { x: 0, y: r - v },
                    middleTop: { x: 0, y: 0 }
                }
                if (ratio > 0) {
                    // towards bottom
                    p.bottomLeft.y = p.bottomLeft.y - v;
                    p.middleBottom.y = p.middleBottom.y + v;
                    p.bottomRight.y = p.bottomRight.y - v;
                }
            }
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

    public valuePath: string = "";
    public outlinePath: string = "";
    public energyFlow: EnergyFlow;
    public square: SvgSquare;
    public squarePosition: SvgSquarePosition;
    public name: string = "";

    protected valueRatio: number = 0;
    protected valueText: string = "";
    protected valueText2: string = "";
    protected innerRadius: number = 0;
    protected outerRadius: number = 0;
    protected height: number = 0;
    protected width: number = 0;
    protected pulsetime = 2000;
    protected lastValue = { valueAbsolute: 0, valueRatio: 0, sumRatio: 0 };

    constructor(
        translateName: string,
        protected direction: "left" | "right" | "down" | "up" = "left",
        protected startAngle: number,
        protected endAngle: number,
        public color: string,
        protected translate: TranslateService
    ) {
        this.name = translate.instant(translateName);
        this.energyFlow = new EnergyFlow(direction, 0);
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
        this.energyFlow.update(sumRatio);
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
        this.energyFlow = new EnergyFlow(this.direction, availableInnerRadius);

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