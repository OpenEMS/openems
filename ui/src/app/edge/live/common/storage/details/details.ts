import { Component } from "@angular/core";
import { FormsModule, ReactiveFormsModule } from "@angular/forms";
import { FormlyModule } from "@ngx-formly/core";
import { CommonUiModule } from "src/app/shared/common-ui.module";
import { ComponentsBaseModule } from "src/app/shared/components/components.module";
import { HelpButtonComponent } from "src/app/shared/components/modal/help-button/help-button";
import { ModalComponentsModule } from "src/app/shared/components/modal/modal.module";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView, ViewContext } from "src/app/shared/components/shared/oe-formly-component";
import { PipeComponentsModule } from "src/app/shared/pipe/pipe.module";
import { LiveDataServiceProvider } from "src/app/shared/provider/live-data-service-provider";
import { LocaleProvider } from "src/app/shared/provider/locale-provider";
import { EdgeConfig } from "src/app/shared/shared";
import { SharedStorage } from "../shared/shared";

@Component({
    selector: "oe-common-storage-owner-guest-installer-details",
    standalone: true,
    templateUrl: "../../../../../shared/components/formly/formly-field-modal/template.html",
    imports: [
        CommonUiModule,
        HelpButtonComponent,
        ReactiveFormsModule,
        FormsModule,
        FormlyModule,
        PipeComponentsModule,
        ComponentsBaseModule,
        ModalComponentsModule,
        LocaleProvider,
        LiveDataServiceProvider,
    ],
})
export class CommonStorageOwnerGuestInstallerDetailsComponent extends AbstractFormlyComponent {

    protected override formlyWrapper: "formly-field-modal" | "formly-field-navigation" = "formly-field-navigation";

    protected override generateView(viewContext: ViewContext): OeFormlyView {
        const lines: OeFormlyField[] = [];
        const essComponents: EdgeConfig.Component[] = viewContext.config
            .getComponentsImplementingNature("io.openems.edge.ess.api.SymmetricEss")
            .filter(component => {

                return (component.isEnabled || component.getPropertyFromComponent<boolean>("enabled") == true) && !viewContext.config
                    .getNatureIdsByFactoryId(component.factoryId)
                    .includes("io.openems.edge.ess.api.MetaEss");
            });

        lines.push(
            ...SharedStorage.getTotalLines(viewContext.translate, essComponents),
            ...SharedStorage.getEssLines(viewContext.translate, essComponents, viewContext.config),
            {
                type: "info-line",
                name: viewContext.translate.instant("EDGE.INDEX.WIDGETS.PHASES_INFO"),
            }
        );

        return {
            lines: lines,
            title: viewContext.translate.instant("EDGE.HISTORY.PHASE_ACCURATE"),
            component: new EdgeConfig.Component(),
        };
    }
}
