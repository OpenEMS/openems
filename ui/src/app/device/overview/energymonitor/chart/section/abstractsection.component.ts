import { Component, Input, trigger, state, style, transition, animate } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import * as d3 from 'd3';

import { EnergytableComponent } from '../../../energytable/energytable.component';
import { Device } from '../../../../../shared/shared';

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

export class Circle {
    public state: "one" | "two" | "three" = "one";
    constructor(
        public x: number,
        public y: number,
        public radius: number
    ) { }

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

export class CircleDirection {
    constructor(
        public direction: "left" | "right" | "down" | "up"
    ) { }
}

let pulsetime = 1000;

export abstract class AbstractSection {

    public valuePath: string = "";
    public outlinePath: string = "";
    public circles: Circle[] = [];
    public square: SvgSquare;
    public squarePosition: SvgSquarePosition;
    public pulsetimeup: number;
    public pulsetimedown: number;
    public pulsetimeright: number;
    public pulsetimeleft: number;
    public name: string = "";

    protected valueRatio: number = 0;
    protected valueText: string = "";
    protected innerRadius: number = 0;
    protected outerRadius: number = 0;
    protected height: number = 0;
    protected width: number = 0;

    protected lastValue = { absolute: 0, ratio: 0 };

    private setPulsetime(value: number) {
        pulsetime = value;
    }

    constructor(
        translateName: string,
        protected startAngle: number,
        protected endAngle: number,
        public color: string,
        protected translate: TranslateService
    ) {
        this.name = translate.instant(translateName);
    }

    /**
     * This method is called on every change of values.
     */
    public updateValue(absolute: number, ratio: number) {
        this.lastValue = { absolute: absolute, ratio: ratio };
        this.valueRatio = this.getValueRatio(ratio);
        this.valueText = this.getValueText(absolute);
        let valueEndAngle = ((this.endAngle - this.startAngle) * this.valueRatio) / 100 + this.getValueStartAngle();
        let valueArc = this.getArc()
            .startAngle(this.deg2rad(this.getValueStartAngle()))
            .endAngle(this.deg2rad(valueEndAngle));
        this.valuePath = valueArc();
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
         * calculate square
         */
        this.square = this.getSquare(innerRadius);
        this.squarePosition = this.getSquarePosition(this.square, innerRadius);

        /**
         * Calculate Circles
         */
        let circleDirection = this.getCircleDirection();
        let availableInnerRadius = innerRadius - this.square.image.y - this.square.image.length;
        let radius = Math.round(availableInnerRadius * 0.1);
        let space = {
            min: radius * 2,
            max: innerRadius - this.square.image.y - this.square.image.length - 2 * radius
        }
        let fact = { x: 0, y: 0 };
        if (circleDirection.direction == "left") {
            fact = { x: -1, y: 0 };
        } else if (circleDirection.direction == "right") {
            fact = { x: 1, y: 0 };
        } else if (circleDirection.direction == "up") {
            fact = { x: 0, y: -1 };
        } else if (circleDirection.direction == "down") {
            fact = { x: 0, y: 1 };
        }
        let noOfCircles = 3;
        this.circles = [];
        for (let i = 0; i <= 1; i = i + 1 / (noOfCircles - 1)) {
            this.circles.push(new Circle(((space.max - space.min) * i + space.min) * fact.x, ((space.max - space.min) * i + space.min) * fact.y, radius));
        }

        // now update also the value specific elements
        this.updateValue(this.lastValue.absolute, this.lastValue.ratio);
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
    protected abstract getCircleDirection(): CircleDirection;
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