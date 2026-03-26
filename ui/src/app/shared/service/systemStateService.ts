import { HttpClient } from "@angular/common/http";
import { inject, Injectable, signal } from "@angular/core";

@Injectable({
    providedIn: "root",
})
export class SystemStateService {

    private _isSystemOutage = signal<boolean>(false);
    private http: HttpClient = inject(HttpClient);

    public get isSystemOutage() {
        return this._isSystemOutage.asReadonly();
    }

    private setSystemOutage(value: boolean) {
        this._isSystemOutage.set(value);
    }
}
