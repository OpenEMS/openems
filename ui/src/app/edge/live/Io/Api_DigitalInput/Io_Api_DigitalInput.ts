import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { EdgeConfig } from "src/app/shared/shared";

import { Io_Api_DigitalInput_ModalComponent } from "./modal/MODAL.COMPONENT";

@Component({
    selector: "Io_Api_DigitalInput",
    templateUrl: "./Io_Api_DigitalInput.html",
    standalone: false,
})

export class Io_Api_DigitalInputComponent extends AbstractFlatWidget {

    public ioComponents: EDGE_CONFIG.COMPONENT[] | null = null;
    public ioComponentCount = 0;

    async presentModal() {
        const modal = await THIS.MODAL_CONTROLLER.CREATE({
            component: Io_Api_DigitalInput_ModalComponent,
            componentProps: {
                edge: THIS.EDGE,
                ioComponents: THIS.IO_COMPONENTS,
            },
        });
        return await MODAL.PRESENT();
    }

    protected override afterIsInitialized(): void {
        THIS.SERVICE.GET_CONFIG().then(config => {
            THIS.IO_COMPONENTS = CONFIG.GET_COMPONENTS_IMPLEMENTING_NATURE("IO.OPENEMS.EDGE.IO.API.DIGITAL_INPUT").filter(component => COMPONENT.IS_ENABLED);
            THIS.IO_COMPONENT_COUNT = THIS.IO_COMPONENTS.LENGTH;
        });
    }

}
