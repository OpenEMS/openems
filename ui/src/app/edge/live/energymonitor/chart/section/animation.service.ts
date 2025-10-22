import { Injectable } from '@angular/core';
import { BehaviorSubject, interval } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class AnimationService {
    private readonly animationSpeed = 1150;
    private toggleAnimSubject = new BehaviorSubject(true);

    toggleAnimation$ = this.toggleAnimSubject.asObservable();
    private value: boolean = true;

    constructor() {
        interval(this.animationSpeed).subscribe(() => {
            this.value = !this.value;
            this.toggleAnimSubject.next(this.value);
        });
    }
}
