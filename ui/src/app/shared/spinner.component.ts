import { Component, Input, trigger, state, style, transition, animate, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';

// spinner
import { SpinnerService } from 'angular-spinners';

let pulsetime = 1000;
let pulsetimedown = 2000;

@Component({
    selector: 'spinner-animation',
    templateUrl: './spinner.component.html',
    animations: [
        trigger('spinner', [
            state('one', style({
                height: "30px",
                width: "30px"
            })),
            state('two', style({
                height: "60px",
                width: "60px"
            })),
            transition('one => two', animate(pulsetime + 'ms')),
            transition('two => one', animate(pulsetime + 'ms'))
        ])
    ]
})
export class SpinnerComponent implements OnInit, OnDestroy {
    public state: "one" | "two" = "two";
    public loading: boolean = true;

    private subscription: Subscription = new Subscription();

    constructor(
        protected spinnerService: SpinnerService
    ) { }

    ngOnInit() {
        this.spinnerService.show('loadingSpinner');
        this.switchState();
        this.subscription = Observable.interval(pulsetime).subscribe(() => {
            this.switchState();
        });
    }

    ngOnDestroy() {
        this.subscription.unsubscribe();
    }

    public switchState() {
        if (this.state == 'one') {
            this.state = 'two';
        } else if (this.state == 'two') {
            this.state = 'one';
        } else {
            this.state = 'one';
        }
    }

}