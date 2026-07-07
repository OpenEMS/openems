import { Component } from "@angular/core";
import { AbstractFlatWidget } from "src/app/shared/components/flat/abstract-flat-widget";
import { Modal } from "src/app/shared/components/flat/flat";
import { EdgeConfig } from "src/app/shared/shared";

import { Io_Api_DigitalInput_ModalComponent } from "./modal/modal.component";

@Component({
    selector: "Io_Api_DigitalInput",
    templateUrl: "./Io_Api_DigitalInput.html",
    standalone: false,
})

export class Io_Api_DigitalInputComponent extends AbstractFlatWidget {

    public ioComponents: EdgeConfig.Component[] | null = null;
    public ioComponentCount = 0;

    protected modalComponent: Modal | null = null;

    protected getModalComponent(): Modal {
        return {
            component: Io_Api_DigitalInput_ModalComponent,
            componentProps: {
                ioComponents: this.ioComponents,
                edge: this.edge,
            },
        };
    }

    protected override afterIsInitialized(): void {
        this.modalComponent = this.getModalComponent();

        this.service.getConfig().then(config => {
            this.ioComponents = config.getComponentsImplementingNature("io.openems.edge.io.api.DigitalInput").filter(component => component.isEnabled);
            this.ioComponentCount = this.ioComponents.length;
        });
    }

}
