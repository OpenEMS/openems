import * as d3 from 'd3';
import { DeviceOverviewEnergytableComponent } from '../../../energytable/energytable.component';
import { Device } from '../../../../../service/device';

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
    constructor(
        public x: number,
        public y: number,
        public radius: number
    ) { }
}

export class CircleDirection {
    constructor(
        public direction: "left" | "right" | "down" | "up"
    ) { }
}

// x = getxxx();
// let multX = 0;
// let multY = 0;
// if(x.direction == "left") {
//     multX = -1;
//     multY = 0;
// }

export abstract class AbstractSection {
    private outlinePath: number = null;
    private valuePath: number = null;
    protected valueRatio: number = 0;
    protected valueText: string = "";
    protected innerRadius: number = 0;
    protected outerRadius: number = 0;
    private textPosition: SvgTextPosition;
    private imagePosition: SvgImagePosition;
    private rect: SvgSquare;
    private rectPosition: SvgSquarePosition;
    protected height: number = 0;
    protected width: number = 0;
    private circles: Circle[] = [];

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

        // Calculate circles
        let circleDirection = this.getCircleDirection();
        let availableInnerRadius = innerRadius - this.rect.image.y - this.rect.image.length;
        let radius = Math.round(availableInnerRadius * 0.1);
        let space = {
            min: radius * 2,
            max: innerRadius - this.rect.image.y - this.rect.image.length - 2 * radius
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
    }

    protected getRect(innerRadius: any): SvgSquare {
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
    protected abstract getRectPosition(rect: SvgSquare, innerRadius: number): SvgSquarePosition;
    protected abstract getCircleDirection(): CircleDirection;
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