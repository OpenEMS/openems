import { Injectable } from '@angular/core';
import { BehaviorSubject, timer } from 'rxjs';

@Injectable({
    providedIn: 'root',
})
export class AnimationService {
    private readonly intervalMs = 1150;
    private toggleAnimSubject = new BehaviorSubject(true);

    toggleAnimation$ = this.toggleAnimSubject.asObservable();
    private value: boolean = true;

    constructor() {
        timer(this.intervalMs, this.intervalMs).subscribe(() => {
            this.value = !this.value;
            this.toggleAnimSubject.next(this.value);
        });
    }
}
