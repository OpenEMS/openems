import { Component } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { AbstractModal } from "src/app/shared/components/modal/abstractModal";
import { ChannelAddress } from "src/app/shared/shared";

@Component({
    templateUrl: "./gruenStrom_modal.html",
})
export class GruenStromModalComponent extends AbstractModal {


    protected override getChannelAddresses() {
        const channelAddresses: ChannelAddress[] = [];
        channelAddresses.push(
            new ChannelAddress(this.component.id, "GreenLevel"),
        );
        return channelAddresses;
    }

    protected override getFormGroup(): FormGroup<any> {
        return this.formBuilder.group({
            postalCode: new FormControl(this.component.properties["plz"]),
        });
    }
}
