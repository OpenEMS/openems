import { Component } from "@angular/core";
import { filter, take } from "rxjs";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { EdgeConfig } from "src/app/shared/shared";

@Component({
    templateUrl: "./update-app-config.html",
    standalone: false,
})

export class UpdateAppConfigComponent extends AbstractModal {

    override async updateComponent(config: EdgeConfig) {
        return new Promise<void>((res) => {
            this.route.params.pipe(filter(params => params != null), take(1)).subscribe((params) => {
                this.component = config.getComponent(params.componentId);
                res();
            });
        });
    }
}
