import { Component, trigger, state, style, transition, animate } from '@angular/core';
import { Observable } from "rxjs/Rx";

class Circle {
    public state: "one" | "two" = "one";
    constructor(
        public x: number,
        public y: number
    ) { }

    public switchState() {
        if (this.state == 'one') {
            this.state = 'two';
        } else {
            this.state = 'one';
        }
    }
}

let pulsetime = 1000;
let pulsetimeleft = 2000;
// let pulsetimeright = 3000;
// let pulsetimeup = 750;
// let pulsetimedown = 3000;

@Component({
    selector: 'chart-test',
    templateUrl: './test2.html',
    animations: [
        trigger('circle', [
            state('one', style({
                r: 15,
                fill: 'none',
                stroke: 'lightblue',
                "stroke-width": 5
            })),
            state('two', style({
                r: 15,
                fill: 'none',
                stroke: 'lightblue',
                "stroke-width": 1
            })),
            transition('one => two', animate(pulsetime + 'ms')),
            transition('two => one', animate(pulsetime + 'ms'))
        ])
    ]

})
export class ChartTest {
    constructor() {
        Observable.interval(pulsetimeleft)
            .subscribe(x => {
                this.circles[3].switchState();
                setTimeout(() => {
                    this.circles[2].switchState();
                }, pulsetime / 4);
                setTimeout(() => {
                    this.circles[1].switchState();
                }, pulsetime / 2);
                setTimeout(() => {
                    this.circles[0].switchState();
                }, pulsetime * 3 / 4);
                // setTimeout(() => {
                //     this.circles[0].switchState();
                //     this.circles[1].switchState();
                //     this.circles[2].switchState();
                //     this.circles[3].switchState();
                // }, 1000 + 'ms');
            })



        // Observable.interval(pulsetimeright)
        //     .subscribe(x => {
        //         this.circles[4].switchState();
        //         setTimeout(() => {
        //             this.circles[5].switchState();
        //         }, pulsetime / 4);
        //         setTimeout(() => {
        //             this.circles[6].switchState();
        //         }, pulsetime / 2);
        //         setTimeout(() => {
        //             this.circles[7].switchState();
        //         }, pulsetime * 3 / 4);
        //     })
        // Observable.interval(pulsetimeup)
        //     .subscribe(x => {
        //         this.circles[11].switchState();
        //         setTimeout(() => {
        //             this.circles[10].switchState();
        //         }, pulsetime / 4);
        //         setTimeout(() => {
        //             this.circles[9].switchState();
        //         }, pulsetime / 2);
        //         setTimeout(() => {
        //             this.circles[8].switchState();
        //         }, pulsetime * 3 / 4);
        //     })
        // Observable.interval(pulsetimedown)
        //     .subscribe(x => {
        //         this.circles[12].switchState();
        //         setTimeout(() => {
        //             this.circles[13].switchState();
        //         }, pulsetime / 4);
        //         setTimeout(() => {
        //             this.circles[14].switchState();
        //         }, pulsetime / 2);
        //         setTimeout(() => {
        //             this.circles[15].switchState();
        //         }, pulsetime * 3 / 4);
        //     })
    }

    private setPulsetime(value: number) {
        pulsetime = value;
    }

    public circles: Circle[] = [
        new Circle(-30, 0),
        new Circle(-70, 0),
        new Circle(-110, 0),
        new Circle(-150, 0),
        new Circle(30, 0),
        new Circle(70, 0),
        new Circle(110, 0),
        new Circle(150, 0),
        new Circle(0, -30),
        new Circle(0, -70),
        new Circle(0, -110),
        new Circle(0, -150),
        new Circle(0, 30),
        new Circle(0, 70),
        new Circle(0, 110),
        new Circle(0, 150)
    ];
}



