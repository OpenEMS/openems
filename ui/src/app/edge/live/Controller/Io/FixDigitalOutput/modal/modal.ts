import { Component, Input } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { AssertionUtils } from "src/app/shared/utils/assertions/assertions.utils";

@Component({
    templateUrl: "../../../../../../shared/components/formly/formly-field-modal/template.html",
    standalone: false,
    providers: [
        { provide: DataService, useClass: LiveDataService },
    ],
})
export class ModalComponent extends AbstractFormlyComponent {

    @Input() public component: EdgeConfig.Component | null = null;
    @Input() public edge: Edge | null = null;

    protected isOnChannel: ChannelAddress | null = null;

    public static generateView(translate: TranslateService, component: EdgeConfig.Component | null, edge: Edge | null): OeFormlyView {
        AssertionUtils.assertIsDefined(component);
        AssertionUtils.assertIsDefined(edge);

        const lines: OeFormlyField[] = [
            {
                type: "info-line",
                name: translate.instant("GENERAL.MODE"),
            },
            {
                type: "buttons-from-form-control-line",
                name: translate.instant("GENERAL.MODE"),
                controlName: "isOn",
                buttons: [
                    {
                        name: translate.instant("GENERAL.ON"),
                        value: 1,
                        icon: { color: "success", name: "play-outline", size: "medium" },
                    },
                    {
                        name: translate.instant("GENERAL.OFF"),
                        value: 0,
                        icon: { color: "danger", name: "power-outline", size: "medium" },
                    },
                ],
            }];

        return {
            title: Name.METER_ALIAS_OR_ID(component),
            lines: lines,
            component: component,
            edge: edge,
        };
    }


    protected override async getChannelAddresses(): Promise<ChannelAddress[]> {
        const componentId = this.component?.id ?? null;
        if (!componentId) {
            return Promise.resolve([]);
        }
        this.isOnChannel = new ChannelAddress(componentId, "_PropertyIsOn");
        return Promise.resolve([this.isOnChannel]);
    }

    protected override onCurrentData(currentData: CurrentData): void {

        if (this.form.dirty || !this.isOnChannel || currentData.allComponents[this.isOnChannel.toString()] == null) {
            return;
        }

        const isOn = currentData.allComponents[this.isOnChannel.toString()];
        this.form.controls["isOn"].setValue(isOn);
    }

    protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
        return ModalComponent.generateView(this.translate, this.component, this.edge);
    }

    protected override getFormGroup(): FormGroup {
        return new FormGroup({
            isOn: new FormControl(null),
        });
    }
}
