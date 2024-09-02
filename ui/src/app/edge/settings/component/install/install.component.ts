// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { Edge, EdgeConfig, Service, Utils, Websocket } from "../../../../shared/shared";

@Component({
  selector: ComponentInstallComponent.SELECTOR,
  templateUrl: "./install.component.html",
})
export class ComponentInstallComponent implements OnInit {

  private static readonly SELECTOR = "componentInstall";

  public edge: Edge | null = null;
  public factory: EdgeConfig.Factory | null = null;
  public form = null;
  public model = null;
  public fields: FormlyFieldConfig[] | null = null;

  private factoryId: string | null = null;

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
  ) {
  }

  async ngOnInit() {
    this.factoryId = this.route.snapshot.params["factoryId"];
    this.edge = await this.service.getCurrentEdge();
    const config = await this.service.getConfig();

    const [factory, properties] = await this.edge.getFactoryProperties(this.websocket, this.factoryId);
    this.factory = factory;
    const fields: FormlyFieldConfig[] = [];
    const model = {};
    for (const property of properties) {
      const property_id = property.id.replace(".", "_");
      let defaultValue = property.defaultValue;
      // if the type is an array and there is no defaultValue then set the defaultValue to an empty array
      if (property.schema["type"] === "repeat" && defaultValue === null) {
        defaultValue = [];
      }
      const field: FormlyFieldConfig = {
        key: property_id,
        type: "input",
        templateOptions: {
          label: property.name,
          required: defaultValue === null,
          description: property.description,
        },
      };
      // add Property Schema
      Utils.deepCopy(property.schema, field);
      fields.push(field);
      if (defaultValue != null) {
        model[property_id] = defaultValue;

        // Set the next free Component-ID as defaultValue
        if (property_id == "id" && property.schema["type"] !== "repeat") {
          const thisMatch = defaultValue.match(/^(.*)(\d+)$/);
          if (thisMatch) {
            const thisPrefix = thisMatch[1];
            let highestSuffix = Number.parseInt(thisMatch[2]);
            for (const componentId of Object.keys(config.components)) {
              const componentMatch = componentId.match(/^(.*)(\d+)$/);
              if (componentMatch) {
                const componentPrefix = componentMatch[1];
                if (componentPrefix === thisPrefix) {
                  const componentSuffix = Number.parseInt(componentMatch[2]);
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
  }

  public submit() {
    if (!this.form.valid) {
      this.service.toast("Please fill mandatory fields!", "danger");
      return;
    }
    const properties: { name: string, value: any }[] = [];
    for (const controlKey in this.form.controls) {
      const control = this.form.controls[controlKey];
      if (control.value === null) {
        // ignore 'null' values
        continue;
      }
      const property_id = controlKey.replace("_", ".");
      properties.push({ name: property_id, value: control.value });
    }

    this.edge.createComponentConfig(this.websocket, this.factoryId, properties).then(response => {
      this.form.markAsPristine();
      this.service.toast("Successfully created in instance of " + this.factoryId + ".", "success");
    }).catch(reason => {
      this.service.toast("Error creating an instance of " + this.factoryId + ":" + reason.error.message, "danger");
    });
  }

}
