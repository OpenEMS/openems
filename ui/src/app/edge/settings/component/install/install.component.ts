import { ActivatedRoute } from '@angular/router';
import { Component, OnInit } from '@angular/core';
import { FormGroup } from '@angular/forms';
import { FormlyFieldConfig } from '@ngx-formly/core';
import { Service, Utils, Websocket, EdgeConfig, Edge } from '../../../../shared/shared';
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
    this.service.setCurrentComponent({ languageKey: 'Edge.Config.Index.addComponents' }, this.route).then(edge => {
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
        let defaultValue = property.defaultValue
        // if the type is an array and there is no defaultValue then set the defaultValue to an empty array
        if (property.schema["type"] === 'repeat' && defaultValue === null) {
          defaultValue = []
        }
        let field: FormlyFieldConfig = {
          key: property_id,
          type: 'input',
          templateOptions: {
            label: property.name,
            required: defaultValue === null,
            description: property.description
          }
        }
        // add Property Schema 
        Utils.deepCopy(property.schema, field);
        fields.push(field);
        if (defaultValue != null) {
          model[property_id] = defaultValue;

          // Set the next free Component-ID as defaultValue
          if (property_id == 'id' && property.schema["type"] !== 'repeat') {
            let thisMatch = defaultValue.match(/^(.*)(\d+)$/);
            if (thisMatch) {
              let thisPrefix = thisMatch[1];
              let highestSuffix = Number.parseInt(thisMatch[2]);
              for (let componentId of Object.keys(config.components)) {
                let componentMatch = componentId.match(/^(.*)(\d+)$/);
                if (componentMatch) {
                  let componentPrefix = componentMatch[1];
                  if (componentPrefix === thisPrefix) {
                    let componentSuffix = Number.parseInt(componentMatch[2]);
                    highestSuffix = Math.max(highestSuffix, componentSuffix + 1);
                  }
                }
              }
              model[property_id] = thisPrefix + highestSuffix;
            }
          }
        }
      }
      this.form = new FormGroup({});
      this.fields = fields;
      this.model = model;
    });
  }

  public submit() {
    if (!this.form.valid) {
      this.service.toast("Please fill mandatory fields!", "danger");
      return;
    }
    let properties: { name: string, value: any }[] = [];
    for (let controlKey in this.form.controls) {
      let control = this.form.controls[controlKey];
      if (control.value === null) {
        // ignore 'null' values
        continue;
      }
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