// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { FormlyFieldConfig } from "@ngx-formly/core";
import { Edge, EdgeConfig, Service, Utils, Websocket } from "../../../../shared/shared";

@Component({
  selector: COMPONENT_UPDATE_COMPONENT.SELECTOR,
  templateUrl: "./UPDATE.COMPONENT.HTML",
  standalone: false,
})
export class ComponentUpdateComponent implements OnInit {

  private static readonly SELECTOR = "componentUpdate";

  public edge: Edge | null = null;
  public factory: EDGE_CONFIG.FACTORY | null = null;
  public form: FormGroup | null = null;
  public model = null;
  public fields: FormlyFieldConfig[] | null = null;
  public componentIcon: string | null = null;

  private componentId: string | null = null;

  constructor(
    private route: ActivatedRoute,
    protected utils: Utils,
    private websocket: Websocket,
    private service: Service,
  ) {
  }

  async ngOnInit() {
    THIS.EDGE = await THIS.SERVICE.GET_CURRENT_EDGE();
    const componentId = THIS.ROUTE.SNAPSHOT.PARAMS["componentId"];
    const config = await THIS.SERVICE.GET_CONFIG();
    THIS.COMPONENT_ID = componentId;
    const component = CONFIG.COMPONENTS[componentId];
    THIS.COMPONENT_ICON = CONFIG.GET_FACTORY_ICON(THIS.FACTORY, THIS.SERVICE.TRANSLATE);
    const fields: FormlyFieldConfig[] = [];
    const model = {};

    const [factory, properties] = await THIS.EDGE.GET_FACTORY_PROPERTIES(THIS.WEBSOCKET, COMPONENT.FACTORY_ID);
    THIS.FACTORY = factory;

    for (const property of properties) {
      if (PROPERTY.ID === "id") {
        continue; // ignore Component-ID
      }
      const property_id = PROPERTY.ID.REPLACE(".", "_");
      const field: FormlyFieldConfig = {
        key: property_id,
        type: "input",
        templateOptions: {
          label: PROPERTY.NAME,
          description: PROPERTY.DESCRIPTION,
          required: PROPERTY.IS_REQUIRED,
        },
      };
      // add Property Schema
      UTILS.DEEP_COPY(PROPERTY.SCHEMA, field);
      FIELDS.PUSH(field);
      if (COMPONENT.PROPERTIES[PROPERTY.ID]) {

        // filter arrays with nested objects
        if (ARRAY.IS_ARRAY(COMPONENT.PROPERTIES[PROPERTY.ID]) && COMPONENT.PROPERTIES[PROPERTY.ID]?.length > 0 && COMPONENT.PROPERTIES[PROPERTY.ID]?.every(element => typeof element === "object")) {

          // Stringify json for objects nested inside an array
          model[property_id] = JSON.STRINGIFY(COMPONENT.PROPERTIES[PROPERTY.ID]);
        } else {
          model[property_id] = COMPONENT.PROPERTIES[PROPERTY.ID];
        }
      }
    }
    THIS.FORM = new FormGroup({});
    THIS.FIELDS = fields;
    THIS.MODEL = model;
  }

  public submit() {
    const properties: { name: string, value: any }[] = [];
    for (const controlKey in THIS.FORM.CONTROLS) {
      const control = THIS.FORM.CONTROLS[controlKey];
      if (CONTROL.DIRTY) {
        const property_id = CONTROL_KEY.REPLACE("_", ".");
        PROPERTIES.PUSH({ name: property_id, value: CONTROL.VALUE });
      }
    }
    THIS.EDGE.UPDATE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.COMPONENT_ID, properties).then(() => {
      THIS.FORM.MARK_AS_PRISTINE();
      THIS.SERVICE.TOAST("Successfully updated " + THIS.COMPONENT_ID + ".", "success");
    }).catch(reason => {
      THIS.SERVICE.TOAST("Error updating " + THIS.COMPONENT_ID + ":" + REASON.ERROR.MESSAGE, "danger");
    });
  }

  public delete() {
    THIS.EDGE.DELETE_COMPONENT_CONFIG(THIS.WEBSOCKET, THIS.COMPONENT_ID).then(() => {
      THIS.FORM.MARK_AS_PRISTINE();
      THIS.SERVICE.TOAST("Successfully deleted " + THIS.COMPONENT_ID + ".", "success");
    }).catch(reason => {
      THIS.SERVICE.TOAST("Error deleting " + THIS.COMPONENT_ID + ":" + REASON.ERROR.MESSAGE, "danger");
    });
  }

}
