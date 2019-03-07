# REST-Api Controller

## Endpoint '/rest/channel/{Component-ID}/{Channel-ID}'

### GET

### POST

```
{
  "value": 1000
}
```

## Endpoint '/jsonrpc'

Properties 'id' and 'jsonrpc' can be omitted, as they are not required for HTTP POST calls.

### getEdgeConfig

```
{
  "method": "getEdgeConfig",
  "params": {}
}
```

### componentJsonApi

#### getModbusProtocol

```
{
  "method":"componentJsonApi",
  "params":{
    "componentId":"ctrlApiModbusTcp0",
    "payload":{
      "method":"getModbusProtocol",
      "params":{

      }
    }
  }
}
```

### updateComponentConfig

```
{
	"method": "updateComponentConfig",
	"params": {
		"componentId": "ctrlDebugLog0",
		"properties": [{
 			"name": "enabled",
			"value": true
		}]
	}
}
```