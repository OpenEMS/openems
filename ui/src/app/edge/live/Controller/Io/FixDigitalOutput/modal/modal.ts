import { Component, Input } from "@angular/core";
import { FormControl, FormGroup } from "@angular/forms";
import { TranslateService } from "@ngx-translate/core";
import { LiveDataService } from "src/app/edge/live/livedataservice";
import { DataService } from "src/app/shared/components/shared/dataservice";
import { Name } from "src/app/shared/components/shared/name";
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from "src/app/shared/components/shared/oe-formly-component";
import { ChannelAddress, CurrentData, Edge, EdgeConfig } from "src/app/shared/shared";
import { Role } from "src/app/shared/type/role";
import { AssertionUtils } from "src/app/shared/utils/assertions/ASSERTIONS.UTILS";

@Component({
  templateUrl: "../../../../../../shared/components/formly/formly-field-modal/TEMPLATE.HTML",
  standalone: false,
  providers: [
    { provide: DataService, useClass: LiveDataService },
  ],
})
export class ModalComponent extends AbstractFormlyComponent {

  @Input() public component: EDGE_CONFIG.COMPONENT | null = null;
  @Input() public edge: Edge | null = null;

  protected isOnChannel: ChannelAddress | null = null;

  public static generateView(translate: TranslateService, component: EDGE_CONFIG.COMPONENT | null, edge: Edge | null): OeFormlyView {
    ASSERTION_UTILS.ASSERT_IS_DEFINED(component);
    ASSERTION_UTILS.ASSERT_IS_DEFINED(edge);

    const lines: OeFormlyField[] = [
      {
        type: "info-line",
        name: TRANSLATE.INSTANT("GENERAL.MODE"),
      },
      {
        type: "buttons-from-form-control-line",
        name: TRANSLATE.INSTANT("GENERAL.MODE"),
        controlName: "isOn",
        buttons: [
          {
            name: TRANSLATE.INSTANT("GENERAL.ON"),
            value: 1,
            icon: { color: "success", name: "power-outline", size: "medium" },
          },
          {
            name: TRANSLATE.INSTANT("GENERAL.OFF"),
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
    const componentId = THIS.COMPONENT?.id ?? null;
    if (!componentId) {
      return PROMISE.RESOLVE([]);
    }
    THIS.IS_ON_CHANNEL = new ChannelAddress(componentId, "_PropertyIsOn");
    return PROMISE.RESOLVE([THIS.IS_ON_CHANNEL]);
  }

  protected override onCurrentData(currentData: CurrentData): void {

    if (THIS.FORM.DIRTY || !THIS.IS_ON_CHANNEL || CURRENT_DATA.ALL_COMPONENTS[THIS.IS_ON_CHANNEL.TO_STRING()] == null) {
      return;
    }

    const isOn = CURRENT_DATA.ALL_COMPONENTS[THIS.IS_ON_CHANNEL.TO_STRING()];
    THIS.FORM.CONTROLS["isOn"].setValue(isOn);
  }

  protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
    return MODAL_COMPONENT.GENERATE_VIEW(THIS.TRANSLATE, THIS.COMPONENT, THIS.EDGE);
  }

  protected override getFormGroup(): FormGroup {
    return new FormGroup({
      isOn: new FormControl(null),
    });
  }
}
