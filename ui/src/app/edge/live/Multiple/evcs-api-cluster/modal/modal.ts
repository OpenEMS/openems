import { Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { ItemReorderCustomEvent } from "@ionic/angular";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { EdgeConfig } from "src/app/shared/shared";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";
import { FormUtils } from "src/app/shared/utils/form/form.utils";

@Component({
    selector: "oe-multiple-modal",
    templateUrl: "./modal.html",
    standalone: false,
})
export class ModalComponent extends AbstractModal {
    protected evcss: { [evcsId: string]: EdgeConfig.Component } = {};

    protected override getFormGroup(): FormGroup {
        AssertionUtils.assertIsDefined(this.component);
        return this.formBuilder.group({
            "evcs.ids": new FormControl(this.component.getPropertyFromComponent("evcs.ids")),
        });
    }

    protected override onIsInitialized(): void {
        const evcsIds = this!.component!.getPropertyFromComponent<string[]>("evcs.ids");

        if (evcsIds == null) {
            return;
        }

        this.service.getConfig().then(config => {
            evcsIds.forEach(evcsId => {
                const component = config.getComponent(evcsId);
                if (component == null) {
                    return;
                }
                this.evcss[evcsId] = component;
            });
        });
    }

    /**
     * Handles the reorder event on a {@link ItemReorderCustomEvent} for the EVCS priorization list.
     * @param event the reorder event
     * @returns
     */
    protected onReorder(event: ItemReorderCustomEvent) {
        let evcsIds = Object.entries(this.evcss).map(([k, _v]) => k);
        evcsIds = event.detail.complete(evcsIds);
        const control = FormUtils.findFormControlSafely(this.formGroup, "evcs.ids");

        if (control == null) {
            return;
        }

        control.setValue(evcsIds);
        control.markAsDirty();
        this.evcss = evcsIds.reduce((obj, evcsId) => {
            obj[evcsId] = this.evcss[evcsId];
            return obj;
        }, {} as { [evcsId: string]: EdgeConfig.Component });
    }
}
