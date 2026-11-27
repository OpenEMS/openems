import { Injectable } from "@angular/core";

@Injectable({
    providedIn: "root",
})
export class RequestDeduplicator {
    private readonly pending = new Map<string, Promise<unknown>>();

    public run<T>(key: string, requestFactory: () => Promise<T>): Promise<T> {
        const existingRequest = this.pending.get(key);

        if (existingRequest) {
            return existingRequest as Promise<T>;
        }

        const request = requestFactory().finally(() => {
            this.pending.delete(key);
        });

        this.pending.set(key, request);
        return request;
    }
}
