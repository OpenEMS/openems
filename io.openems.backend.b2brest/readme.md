# Backend-to-Backend REST-Api

## Endpoint '/jsonrpc'

Properties 'id' and 'jsonrpc' can be omitted, as they are not required for HTTP POST calls.

### getEdgesStatus

```
{
  "method": "getEdgesStatus",
  "params": {}
}
```

### getEdgesChannelsValues

```
{
  "method":"getEdgesChannelsValues",
  "params": {
    "ids": [
      "edge0"
    ],
    "channels": [
      "_sum/State"
    ]
  }
}
```
