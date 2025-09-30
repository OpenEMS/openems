// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";
import { UserService } from "src/app/shared/service/USER.SERVICE";
import { Edge, Service, Websocket } from "src/app/shared/shared";
import { environment } from "src/environments";

@Component({
  selector: JSONRPC_TEST_COMPONENT.SELECTOR,
  templateUrl: "./JSONRPCTEST.HTML",
  standalone: false,
})
export class JsonrpcTestComponent implements OnInit {

  private static readonly SELECTOR = "jsonrpcTest";

  protected endpoints: Endpoint[] = [];
  private edge: Edge | undefined;

  constructor(
    private route: ActivatedRoute,
    private service: Service,
    private websocket: Websocket,
    private userService: UserService,
  ) {

  }

  public ngOnInit(): void {
    THIS.SERVICE.GET_CURRENT_EDGE().then(edge => {
      THIS.EDGE = edge;
      EDGE.SEND_REQUEST(THIS.WEBSOCKET, new JsonrpcRequest("routes", {})).then(response => {
        THIS.ENDPOINTS = (RESPONSE.RESULT["endpoints"] as EndpointResponse[]).map(endpoint => {
          return {
            method: ENDPOINT.METHOD,
            description: ENDPOINT.DESCRIPTION ? ENDPOINT.DESCRIPTION.REPLACE("\n", "<br>") : null,
            tags: ENDPOINT.TAGS,
            guards: ENDPOINT.GUARDS,
            request: ENDPOINT.REQUEST, // JSON.STRINGIFY(ENDPOINT.REQUEST.JSON, null, 2),
            response: ENDPOINT.RESPONSE,
            parent: ENDPOINT.PARENT,
            requestMethod: "raw",
            form: new FormGroup({}),
            model: {},
            modelRaw: JSON.STRINGIFY(createDummyRequest(ENDPOINT.REQUEST.JSON), null, 2),
          };
        });
      });
    }).catch(e => {
      THIS.SERVICE.TOAST(e, "danger");
    });
  }

  protected request(endpoint: Endpoint): void {
    CONSOLE.LOG("Run!", endpoint);
    if (!ENDPOINT.FETCH) {
      ENDPOINT.FETCH = {};
    }
    ENDPOINT.FETCH.LOADING = true;
    THIS.SERVICE.START_SPINNER_TRANSPARENT_BACKGROUND(ENDPOINT.METHOD);

    let request: JsonrpcRequest = new JsonrpcRequest(
      ENDPOINT.METHOD,
      ENDPOINT.MODEL_RAW ? JSON.PARSE(ENDPOINT.MODEL_RAW) : {},
    );
    for (let i = ENDPOINT.PARENT.LENGTH - 1; i >= 0; i--) {
      const parent = ENDPOINT.PARENT[i];
      if (ENVIRONMENT.BACKEND === "OpenEMS Backend") {
        if (PARENT.METHOD === "authenticatedRpc") {
          break;
        }
      }

      const lastRequest = request;
      request = new JsonrpcRequest(
        PARENT.METHOD, {
        ...PARENT.REQUEST.BASE,
      },
      );
      const lastObj = REQUEST.PARAMS;
      for (let j = 0; j < PARENT.REQUEST.PATH_TO_SUBREQUEST.LENGTH; j++) {
        const path = PARENT.REQUEST.PATH_TO_SUBREQUEST[j];
        if (j === PARENT.REQUEST.PATH_TO_SUBREQUEST.LENGTH - 1) {
          lastObj[path] = lastRequest;
        } else {
          lastObj[path] = {};
        }
      }
    }


    (ENVIRONMENT.BACKEND === "OpenEMS Edge"
      ? THIS.WEBSOCKET.SEND_REQUEST(request)
      : THIS.EDGE.SEND_REQUEST(THIS.WEBSOCKET, request))
      .then(response => {
        ENDPOINT.FETCH.RESPONSE = JSON.STRINGIFY(response, null, 2);
      }).catch(error => {
        ENDPOINT.FETCH.RESPONSE = JSON.STRINGIFY(error, null, 2);
      }).finally(() => {
        ENDPOINT.FETCH.LOADING = false;
        THIS.SERVICE.STOP_SPINNER(ENDPOINT.METHOD);
      });
  }

}

function createDummyRequest(endpointType?: ElementDefinition) {
  if (!endpointType) {
    return undefined;
  }
  switch (ENDPOINT_TYPE.TYPE) {
    case "object": {
      const obj = {};
      for (const [key, value] of OBJECT.ENTRIES(ENDPOINT_TYPE.PROPERTIES)) {
        obj[key] = createDummyRequest(value);
      }
      return obj;
    }
    case "string": {
      return "string";
    }
  }
}

type EndpointResponse = {
  method: string,
  description: string,
  tags: Tag[],
  guards: Guard[],
  request: {
    json: ElementDefinition,
    examples: RequestExample[]
  },
  response: {
    json: ElementDefinition,
    examples: RequestExample[]
  },
  parent: { method: string, request: { base: any, pathToSubrequest: string[] } }[],
};

type Tag = {
  name: string
};

type Guard = {
  name: string,
  description: string
};

type RequestExample = {
  key: string,
  value: {}
};

type ElementDefinition =
  { type: "object", optional: boolean, properties: { [key: string]: ElementDefinition } }
  | { type: "array", optional: boolean, elementType: ElementDefinition }
  | { type: "string" | "boolean" | "number", optional: boolean };

type Endpoint = {
  method: string,
  description: string,
  tags: Tag[],
  guards: Guard[],
  request: {
    json: ElementDefinition,
    examples: RequestExample[],
    selectedExample?: string,
  },
  response: {
    json: ElementDefinition,
    examples: RequestExample[],
  },
  parent: { method: string, request: { base: any, pathToSubrequest: string[] } }[],
  tryRequest?: boolean,
  requestMethod: string,
  form: FormGroup;
  model: any;
  modelRaw: string;
  fetch?: {
    loading?: boolean,
    response?: string;
  }
};
