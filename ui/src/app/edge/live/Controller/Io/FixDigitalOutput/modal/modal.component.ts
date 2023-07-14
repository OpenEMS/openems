import { Component, Input } from '@angular/core';
import { FormControl, FormGroup } from '@angular/forms';
import { TranslateService } from '@ngx-translate/core';
import { AbstractFormlyComponent, OeFormlyField, OeFormlyView } from 'src/app/shared/genericComponents/shared/oe-formly-component';
import { CurrentData, Edge, EdgeConfig } from 'src/app/shared/shared';
import { Role } from 'src/app/shared/type/role';

@Component({
  templateUrl: '../../../../../../shared/formly/formly-field-modal/template.html'
})
export class ModalComponent extends AbstractFormlyComponent {

  @Input() public edge: Edge;
  @Input() public component: EdgeConfig.Component;

  protected override generateView(config: EdgeConfig, role: Role): OeFormlyView {
    return ModalComponent.generateView(config, role, this.translate, this.component, this.form);
  }

  public static getFormGroup() {
    // return [{controlName: 'mode', channel: 'component.id + '/_PropertyIsOn''}]
    return new FormGroup({
      mode: new FormControl(true)
    });
  }

  public static generateView(config: EdgeConfig, role: Role, translate: TranslateService, component: EdgeConfig.Component, formGroup: FormGroup): OeFormlyView {
    let lines: OeFormlyField[] = [
      {
        type: 'children-line',
        children: null,
        name: translate.instant('General.mode')
      },
      {
        type: 'buttons-line',
        buttons: [
          {
            name: translate.instant('General.on'),
            value: "true",
            icons: { color: "success", size: "small", name: "power-outline" },
          },
          {
            name: translate.instant('General.off'),
            value: "false",
            icons: { color: "danger", size: "small", name: "power-outline" },
          }
        ],
        controlName: 'mode',
        formControlValues: (currentData: CurrentData) => {
          return currentData.allComponents[component.id + '/_PropertyIsOn']
        },
        channel: component.id + '/_PropertyIsOn',
      }
    ];

    return {
      title: component.alias,
      lines: lines,
      component: component,
      formToBeBuildt: [{ controlName: 'mode', channel: component.id + '/_PropertyIsOn' }]
      // formGroup: new FormGroup({
      //   mode: new FormControl(component.properties.isOn)
      // })
    };
  }


  // constructor(
  //   public service: Service,
  //   protected translate: TranslateService,
  //   public modalCtrl: ModalController,
  //   public router: Router,
  //   public websocket: Websocket
  // ) { }

  /**  
   * Updates the 'isOn'-Property of the FixDigitalOutput-Controller.
   * 
   * @param event 
   */
  updateMode(event: CustomEvent) {
    let oldMode = this.component.properties.isOn;

    // ion-segment button only supports string as type
    // https://ionicframework.com/docs/v4/api/segment-button

    let newMode = (event.detail.value.toLowerCase() === 'true');

    this.edge.updateComponentConfig(this.websocket, this.component.id, [
      { name: 'isOn', value: newMode }
    ]).then(() => {
      this.component.properties.isOn = newMode;
      this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
    }).catch(reason => {
      this.component.properties.isOn = oldMode;
      this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
      console.warn(reason);
    });
  }
}