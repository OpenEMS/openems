import { Observable } from "rxjs";
import { CancellationToken } from "./cancellation-token";

describe("CancellationToken", () => {

    it("should cancel correctly", () => {

        const cancellationToken = new CancellationToken();
        expect(cancellationToken.isRunning()).toBe(true);
        expect(cancellationToken.isCancelled()).toBe(false);

        let wasCancelled = false;
        cancellationToken.onCancel(() => {
            wasCancelled = true;
        });
        expect(wasCancelled).toBe(false);

        cancellationToken.cancel();
        expect(wasCancelled).toBe(true);
        expect(cancellationToken.isCancelled()).toBe(true);
        expect(cancellationToken.isRunning()).toBe(false);

    });

    it("Observable pipe", () => {
        const cancellationToken = new CancellationToken();
        const observer = new Observable<string>(observer => {
            observer.next("Value 1");
            cancellationToken.cancel();
            observer.next("Value 2");
            observer.complete();
        });

        let lastReceivedValue = "";
        observer.pipe(cancellationToken.observablePipe()).subscribe(value => {
            lastReceivedValue = value;
        });

        expect(lastReceivedValue).toBe("Value 1");
        expect(cancellationToken.isCancelled()).toBe(true);
    });

});
