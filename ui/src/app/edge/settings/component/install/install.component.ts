import { Component, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { Service, Utils, Websocket, EdgeConfig, Edge } from '../../../../shared/shared';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { TranslateService } from '@ngx-translate/core';

@Component({
  selector: ComponentInstallComponent.SELECTOR,
  templateUrl: './install.component.html'
})
export class ComponentInstallComponent implements OnInit {

  private static readonly SELECTOR = "componentInstall";

  public edge: Edge = null;
  public factory: EdgeConfig.Factory = null;
  public form = null;
  public model = null;
  public fields: FormlyFieldConfig[] = null;

  private factoryId: string = null;

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
    private translate: TranslateService
  ) {
  }

  ngOnInit() {
    this.service.setCurrentComponent(this.translate.instant('Edge.Config.Index.AddComponents'), this.route).then(edge => {
      this.edge = edge;
    });
    let factoryId = this.route.snapshot.params["factoryId"];
    this.service.getConfig().then(config => {
      this.factoryId = factoryId;
      this.factory = config.factories[factoryId];
      let fields: FormlyFieldConfig[] = [];
      let model = {};
      for (let property of this.factory.properties) {
        let property_id = property.id.replace('.', '_');
        let field: FormlyFieldConfig = {
          key: property_id,
          type: 'input',
          templateOptions: {
            label: property.name,
            description: property.description
          }
        }
        // add Property Schema 
        Utils.deepCopy(property.schema, field);
        fields.push(field);
        if (property.defaultValue != null) {
          model[property_id] = property.defaultValue;
        }
      }
      this.form = new FormGroup({});
      this.fields = fields;
      this.model = model;
    });
  }

  public submit() {
    let properties: { name: string, value: any }[] = [];
    for (let controlKey in this.form.controls) {
      let control = this.form.controls[controlKey];
      let property_id = controlKey.replace('_', '.');
      properties.push({ name: property_id, value: control.value });
    }

    this.edge.createComponentConfig(this.websocket, this.factoryId, properties).then(response => {
      this.form.markAsPristine();
      this.service.toast("Successfully created in instance of " + this.factoryId + ".", 'success');
    }).catch(reason => {
      this.service.toast("Error creating an instance of " + this.factoryId + ":" + reason.error.message, 'danger');
    });
  }

}