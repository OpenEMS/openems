import { Component, Input } from '@angular/core';
import { FormBuilder, FormControl, FormGroup } from '@angular/forms';
import { ModalController } from '@ionic/angular';
import { TranslateService } from '@ngx-translate/core';
import { Edge, EdgeConfig, Service, Websocket } from '../../../../shared/shared';

@Component({
  selector: FixActivePowerModalComponent.SELECTOR,
  templateUrl: './modal.component.html'
})
export class FixActivePowerModalComponent {

  @Input() public edge: Edge | null = null;
  @Input() public component: EdgeConfig.Component | null = null;

  private static readonly SELECTOR = "fixactivepower-modal";

  public formGroup: FormGroup;
  public loading: boolean = false;

  constructor(
    public modalCtrl: ModalController,
    public service: Service,
    public formBuilder: FormBuilder,
    public websocket: Websocket,
    public translate: TranslateService,
  ) { }

  ngOnInit() {
    this.formGroup = this.formBuilder.group({
      mode: new FormControl(this.component.properties.mode),
      power: new FormControl(this.component.properties.power),
    })
  }

  public updateControllerMode(event: CustomEvent) {
    let oldMode = this.component.properties['mode'];
    let newMode = event.detail.value;

    if (this.edge != null) {
      this.edge.updateComponentConfig(this.websocket, this.component.id, [
        { name: 'mode', value: newMode }
      ]).then(() => {
        this.component.properties.mode = newMode;
        this.formGroup.markAsPristine();
        this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
      }).catch(reason => {
        this.component.properties.mode = oldMode;
        this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
        console.warn(reason);
      });
    }
  }

  applyChanges() {
    if (this.edge != null) {
      if (this.edge.roleIsAtLeast('owner')) {
        let updateComponentArray = [];
        Object.keys(this.formGroup.controls).forEach((element, index) => {
          if (this.formGroup.controls[element].dirty) {
            updateComponentArray.push({ name: Object.keys(this.formGroup.controls)[index], value: this.formGroup.controls[element].value })
          }
        })
        this.loading = true;
        this.edge.updateComponentConfig(this.websocket, this.component.id, updateComponentArray).then(() => {
          this.component.properties.mode = this.formGroup.controls['mode'].value;
          this.component.properties.power = this.formGroup.controls['power'].value;
          this.loading = false;
          this.service.toast(this.translate.instant('General.changeAccepted'), 'success');
        }).catch(reason => {
          this.formGroup.controls['mode'].setValue(this.component.properties.mode);
          this.formGroup.controls['power'].setValue(this.component.properties.power);
          this.loading = false;
          this.service.toast(this.translate.instant('General.changeFailed') + '\n' + reason.error.message, 'danger');
          console.warn(reason);
        })
        this.formGroup.markAsPristine()
      } else {
        this.service.toast(this.translate.instant('General.insufficientRights'), 'danger');
      }
    }
  }
}