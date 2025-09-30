// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { Edge, EdgeConfig, Service, Utils, Websocket } from "../../../../shared/shared";

@Component({
  selector: COMPONENT_INSTALL_COMPONENT.SELECTOR,
  templateUrl: "./INSTALL.COMPONENT.HTML",
  standalone: false,
})
export class ComponentInstallComponent implements OnInit {

  private static readonly SELECTOR = "componentInstall";

  public edge: Edge | null = null;
  public factory: EDGE_CONFIG.FACTORY | null = null;
  public form = null;
  public model = null;
  public fields: FormlyFieldConfig[] | null = null;
  public componentIcon: string | null = null;

  private factoryId: string | null = null;

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
  ) {
  }

  async ngOnInit() {
    THIS.FACTORY_ID = THIS.ROUTE.SNAPSHOT.PARAMS["factoryId"];
    THIS.EDGE = await THIS.SERVICE.GET_CURRENT_EDGE();
    const config = await THIS.SERVICE.GET_CONFIG();
    THIS.COMPONENT_ICON = CONFIG.GET_FACTORY_ICON(THIS.FACTORY, THIS.SERVICE.TRANSLATE);

    const [factory, properties] = await THIS.EDGE.GET_FACTORY_PROPERTIES(THIS.WEBSOCKET, THIS.FACTORY_ID);
    THIS.FACTORY = factory;
    const fields: FormlyFieldConfig[] = [];
    const model = {};
    for (const property of properties) {
      const property_id = PROPERTY.ID.REPLACE(".", "_");
      let defaultValue = PROPERTY.DEFAULT_VALUE;
      // if the type is an array and there is no defaultValue then set the defaultValue to an empty array
      if (PROPERTY.SCHEMA["type"] === "repeat" && defaultValue === null) {
        defaultValue = [];
      }
      const field: FormlyFieldConfig = {
        key: property_id,
        type: "input",
        templateOptions: {
          label: PROPERTY.NAME,
          required: defaultValue === null,
          description: PROPERTY.DESCRIPTION,
        },
      };
      // add Property Schema
      UTILS.DEEP_COPY(PROPERTY.SCHEMA, field);
      FIELDS.PUSH(field);
      if (defaultValue != null) {
        model[property_id] = defaultValue;

        // Set the next free Component-ID as defaultValue
        if (property_id == "id" && PROPERTY.SCHEMA["type"] !== "repeat") {
          const thisMatch = DEFAULT_VALUE.MATCH(/^(.*)(\d+)$/);
          if (thisMatch) {
            const thisPrefix = thisMatch[1];
            let highestSuffix = NUMBER.PARSE_INT(thisMatch[2]);
            for (const componentId of OBJECT.KEYS(CONFIG.COMPONENTS)) {
              const componentMatch = COMPONENT_ID.MATCH(/^(.*)(\d+)$/);
              if (componentMatch) {
                const componentPrefix = componentMatch[1];
                if (componentPrefix === thisPrefix) {
                  const componentSuffix = NUMBER.PARSE_INT(componentMatch[2]);
                  highestSuffix = MATH.MAX(highestSuffix, componentSuffix + 1);
                }
              }
            }
            model[property_id] = thisPrefix + highestSuffix;
          }
        }
      }
    }
    THIS.FORM = new FormGroup({});
    THIS.FIELDS = fields;
    THIS.MODEL = model;
  }

  public submit() {
    if (!THIS.FORM.VALID) {
      THIS.SERVICE.TOAST("Please fill mandatory fields!", "danger");
      return;
    }
    const properties: { name: string, value: any }[] = [];
    for (const controlKey in THIS.FORM.CONTROLS) {
      const control = THIS.FORM.CONTROLS[controlKey];
      if (CONTROL.VALUE === null) {
        // ignore 'null' values
        continue;
      }
      const property_id = CONTROL_KEY.REPLACE("_", ".");
      PROPERTIES.PUSH({ name: property_id, value: CONTROL.VALUE });
    }

    THIS.EDGE.CREATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.FACTORY_ID, properties).then(response => {
      THIS.FORM.MARK_AS_PRISTINE();
      THIS.SERVICE.TOAST("Successfully created in instance of " + THIS.FACTORY_ID + ".", "success");
    }).catch(reason => {
      THIS.SERVICE.TOAST("Error creating an instance of " + THIS.FACTORY_ID + ":" + REASON.ERROR.MESSAGE, "danger");
    });
  }

}
