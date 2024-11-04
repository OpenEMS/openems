// @ts-strict-ignore
import { Component, OnInit } from "@angular/core";
import { FormGroup } from "@angular/forms";
import { ActivatedRoute } from "@angular/router";
import { JsonrpcRequest } from "src/app/shared/jsonrpc/base";
import { Edge, Service, Websocket } from "src/app/shared/shared";
import { environment } from "src/environments";

@Component({
  selector: JsonrpcTestComponent.SELECTOR,
  templateUrl: "./jsonrpctest.html",
})
export class JsonrpcTestComponent implements OnInit {

  private static readonly SELECTOR = "jsonrpcTest";

  protected endpoints: Endpoint[] = [];
  private edge: Edge | undefined;

  constructor(
    private route: ActivatedRoute,
    private service: Service,
    private websocket: Websocket,
  ) {

  }

  public ngOnInit(): void {
    this.service.getCurrentEdge().then(edge => {
      this.edge = edge;
      edge.sendRequest(this.websocket, new JsonrpcRequest("routes", {})).then(response => {
        this.endpoints = (response.result["endpoints"] as EndpointResponse[]).map(endpoint => {
          return {
            method: endpoint.method,
            description: endpoint.description ? endpoint.description.replace("\n", "<br>") : null,
            tags: endpoint.tags,
            guards: endpoint.guards,
            request: endpoint.request, // JSON.stringify(endpoint.request.json, null, 2),
            response: endpoint.response,
            parent: endpoint.parent,
            requestMethod: "raw",
            form: new FormGroup({}),
            model: {},
            modelRaw: JSON.stringify(createDummyRequest(endpoint.request.json), null, 2),
          };
        });
      });
    }).catch(e => {
      this.service.toast(e, "danger");
    });
  }

  protected request(endpoint: Endpoint): void {
    console.log("Run!", endpoint);
    if (!endpoint.fetch) {
      endpoint.fetch = {};
    }
    endpoint.fetch.loading = true;
    this.service.startSpinnerTransparentBackground(endpoint.method);

    let request: JsonrpcRequest = new JsonrpcRequest(
      endpoint.method,
      endpoint.modelRaw ? JSON.parse(endpoint.modelRaw) : {},
    );
    for (let i = endpoint.parent.length - 1; i >= 0; i--) {
      const parent = endpoint.parent[i];
      if (environment.backend === "OpenEMS Backend") {
        if (parent.method === "authenticatedRpc") {
          break;
        }
      }

      const lastRequest = request;
      request = new JsonrpcRequest(
        parent.method, {
        ...parent.request.base,
      },
      );
      const lastObj = request.params;
      for (let j = 0; j < parent.request.pathToSubrequest.length; j++) {
        const path = parent.request.pathToSubrequest[j];
        if (j === parent.request.pathToSubrequest.length - 1) {
          lastObj[path] = lastRequest;
        } else {
          lastObj[path] = {};
        }
      }
    }


    (environment.backend === "OpenEMS Edge"
      ? this.websocket.sendRequest(request)
      : this.edge.sendRequest(this.websocket, request))
      .then(response => {
        endpoint.fetch.response = JSON.stringify(response, null, 2);
      }).catch(error => {
        endpoint.fetch.response = JSON.stringify(error, null, 2);
      }).finally(() => {
        endpoint.fetch.loading = false;
        this.service.stopSpinner(endpoint.method);
      });
  }

}

function createDummyRequest(endpointType?: EndpointType) {
  if (!endpointType) {
    return undefined;
  }
  switch (endpointType.type) {
    case "object": {
      const obj = {};
      for (const [key, value] of Object.entries(endpointType.properties)) {
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
    json: EndpointType,
    examples: RequestExample[]
  },
  response: {
    json: EndpointType,
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

type EndpointType =
  {
    type: "object",
    properties: { [key: string]: EndpointType }
  }
  | {
    type: "string",
    constraints: string[]
  };

type Endpoint = {
  method: string,
  description: string,
  tags: Tag[],
  guards: Guard[],
  request: {
    json: EndpointType,
    examples: RequestExample[],
    selectedExample?: string,
  },
  response: {
    json: EndpointType,
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
