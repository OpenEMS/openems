import { Component, Input, trigger, state, style, transition, animate, OnInit, OnDestroy } from '@angular/core';
import { Subject } from 'rxjs/Subject';
import { Observable } from 'rxjs/Observable';
import { Subscription } from 'rxjs/Subscription';

@Component({
    selector: 'spinner-animation',
    templateUrl: './spinner.component.html'
})
export class SpinnerComponent {
    public loading: boolean = true;

}