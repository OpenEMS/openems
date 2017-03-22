# Communication protocols

This chapter explains the communication protocol used between the different components.

## [1] Browser <-> FemsServer

### [1.1] Authenticate

[1.1.1]
Cookie: session_id

[1.1.2] 
on success:
```
{ result: { devices: ["...",] } }
```
on error:
```
{ error }
```

[1.1.3]
```
{
	authenticate: {
		mode: allow, username, token
	}, metadata: {
		devices: [{
			name, config, online
		}],
		backend: "femsserver"
	}
}
```

### [1.2] Current data

[1.2.1]
```
{
	device: "...",
	subscribe: {
		thing0: [
			channel
		]
	}
}
```

[1.2.2]
```
{
	subscribe: {
		thing0: [
			channel
		]
	}
}
```

[1.2.3]
```
{
	currentdata: [{ 
		channel, value
    }]
}
```

[1.2.4]
```
{
	device: "...",
    currentdata: [{ 
    	channel, value
    }]
}
```

### [1.3] Notification
```
{
	device: "...",
	notification: {
		message: "...
	}
}
```

## [2] OpenEMS <-> FemsServer

### [2.1] Authenticate

[2.1.1] 

[2.1.2]
```
{
	metadata: {
		config: {},
		backend: "openems"
	}
}
```
### [2.2] timestamped data

[2.2.1]
```
{
	timedata: {
		timestamp: [{
			channel, value
		}]
	}
}
```

## [3] Browser <-> OpenEMS

### [3.1] Authenticate

[3.1.1]
```
{
	authenticate: {
		mode: login,
		[password: "...",] 
		[token: "..."]
	}
}
```

[3.1.2]
```
{
	authenticate: {
		mode: allow, username, token
	}, metadata: {
		config: {},
		backend: "openems"
	}
}
```


### [3.2] Current data

[3.2.1]
```
{
	subscribe: {
		thing0: [
			channel
		]
	}
}
```

[3.2.2]
```
{
	currentdata: [{ 
		channel: ...,
		value: ...
    }]
}
```

### [3.3] Configuration

[3.3.1]
```
{
	configure: [{
		mode: "update",
		thing: "...",
		channel: "...",
		value: "..." | { ... }
	}]
}
```

[3.3.2]
```
{
	configure: [{
		mode: "create",
		object: { ... },
		parent: "..."
	}]
}
```

[3.3.3]
```
{
	configure: [{
		mode: "delete",
		thing: "..."
	}]
}
```

[3.3.4]
```
{
 	metadata: {
		config: {}
	}, notification: {
		type: "success" | "error" | "warning" | "info",
		message: "..."
	}
}
```

### [3.4] Query history data

[3.4.1]
```
{
	query: {
		mode: "history",
		fromDate: "01.01.2017",
		toDate: "01.01.2017", 
		timezone: "GMT",
		channels: {
			thing: [channel] 
		}
	}
}
```

[3.4.2]
```
{
    queryreply: {
    	mode: "history",
		fromDate: "2017-01-01",
		toDate: "2017-01-01", 
		timezone: "GMT",
		data: [{
			time: ...,
			channels: {
				'thing': {
					'channel': 'value'
				} 
			}
		}]
	}
}
```

### [3.4] System
```
{
 	system: {
 		mode: "systemd-restart",
 		service: "fems-pagekite" | "..."
 	}
}
```

# Generate the docs

## Charts

Charts are defined in mmd-files and use the "mermaid" tool: 

http://knsv.github.io/mermaid
http://knsv.github.io/mermaid/live_editor