import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { CurrentData } from "src/app/shared/shared";
import { GruenStromModalComponent } from "./modal/gruenStrom_modal";

@Component({
    selector: "GruenStromComponent",
    templateUrl: "./GruenStromComponent.html",
})

export class GruenStromComponent extends AbstractFlatWidget {
    protected greenLevel: any;


    async presentModal() {
        const modal = await this.modalController.create({
            component: GruenStromModalComponent,
            componentProps: {
                component: this.component,
            },
        });
        return await modal.present();
    }

    protected override onCurrentData(currentData: CurrentData) {
        this.greenLevel = currentData.allComponents[this.componentId + "/GreenLevel"];
    }

    protected override afterIsInitialized(): void {
        console.log("test");
        console.log("test");
        console.log("test");
        console.log("test");
        console.log("test");
        console.log("test");
        console.log("test");
    }

}
