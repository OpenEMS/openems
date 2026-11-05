import { ChangeDetectionStrategy, Component, inject, signal } from "@angular/core";
import { FormsModule } from "@angular/forms";
import { ActivatedRoute, Router } from "@angular/router";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";
import { Service } from "src/app/shared/shared";

/**
 * neutrava Passwort-Recovery: ohne Token -> Reset anfordern (Mail-Link),
 * mit ?token -> neues Passwort setzen.
 */
@Component({
    selector: "oe-reset",
    templateUrl: "./reset.component.html",
    changeDetection: ChangeDetectionStrategy.Eager,
    imports: [CommonUiModule, FormsModule],
})
export class ResetComponent {

    protected readonly service = inject(Service);
    private readonly route = inject(ActivatedRoute);
    private readonly router = inject(Router);

    protected token = signal<string | null>(null);
    protected username = signal("");
    protected password = signal("");
    protected password2 = signal("");
    protected done = signal(false);
    protected error = signal<string | null>(null);
    protected busy = signal(false);

    constructor() {
        const t = this.route.snapshot.queryParamMap.get("token");
        if (t) {
            this.token.set(t);
        }
    }

    protected requestReset() {
        this.busy.set(true);
        this.error.set(null);
        this.service.websocket.sendRequest(new JsonrpcRequest("neutravaPublicRequestPasswordReset", {
            username: this.username(),
        })).then(() => {
            this.done.set(true);
        }).catch((e: unknown) => {
            this.done.set(true);
        }).finally(() => this.busy.set(false));
    }

    protected resetPassword() {
        if (this.password() !== this.password2()) {
            this.error.set("Passwörter stimmen nicht überein.");
            return;
        }
        this.busy.set(true);
        this.error.set(null);
        this.service.websocket.sendRequest(new JsonrpcRequest("neutravaPublicResetPassword", {
            token: this.token(),
            password: this.password(),
        })).then(() => {
            this.router.navigate(["/login"], { queryParams: { reset: "done" } });
        }).catch((e: unknown) => {
            this.error.set("Link ungültig oder abgelaufen. Bitte fordere einen neuen Reset-Link an.");
        }).finally(() => this.busy.set(false));
    }
}
