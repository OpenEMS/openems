import { Injectable } from "@angular/core";
import { BehaviorSubject, interval, Observable } from "rxjs";

@Injectable({
    providedIn: "root",
})
export class AnimationService {
    /**
     * Observable to toggle animation states.
     * * true = Show Production/Buy Animation </p>
     * * false = Show Consumption/Sell Animation
     */
    public readonly toggleAnimation$: Observable<boolean>;

    private readonly animationSpeed = 605;
    private toggleAnimSubject = new BehaviorSubject(true);
    private value: boolean = true;

    constructor() {
        this.toggleAnimation$ = this.toggleAnimSubject.asObservable();
        interval(this.animationSpeed).subscribe(() => {
            this.value = !this.value;
            this.toggleAnimSubject.next(this.value);
        });
    }
}
