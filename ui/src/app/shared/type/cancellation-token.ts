import { EffectCleanupRegisterFn, EventEmitter } from "@angular/core";
import { MonoTypeOperatorFunction, Subscription } from "rxjs";
import { createOperatorSubscriber } from "rxjs/internal/operators/OperatorSubscriber";
import { operate } from "rxjs/internal/util/lift";

export class CancellationToken {
    private cancelled = false;
    private cancelEvent = new EventEmitter<void>();

    public static byCleanup(cleanup: EffectCleanupRegisterFn): CancellationToken {
        const cancellationToken = new CancellationToken();
        cleanup(() => {
            cancellationToken.cancel();
        });
        return cancellationToken;
    }

    public onCancel(func: () => void) {
        if (this.cancelled) {
            func();
            return;
        }

        this.cancelEvent.subscribe(func);
    }

    public isCancelled(): boolean {
        return this.cancelled;
    }

    public isRunning(): boolean {
        return !this.cancelled;
    }

    public cancel() {
        if (this.cancelled) {
            return;
        }

        this.cancelled = true;
        this.cancelEvent.emit(undefined);
        this.cancelEvent.unsubscribe();
    }

    public observablePipe<T>(): MonoTypeOperatorFunction<T> {
        return operate((source, subscriber) => {
            const subscription = source.subscribe(
                createOperatorSubscriber(subscriber, (value) => this.isRunning() && subscriber.next(value))
            );
            this.attachToSubscription(subscription);
        });
    }

    public attachToSubscription(subscription: Subscription) {
        if (this.cancelled) {
            subscription.unsubscribe();
        } else {
            this.onCancel(() => {
                subscription.unsubscribe();
            });
        }
    }
}

