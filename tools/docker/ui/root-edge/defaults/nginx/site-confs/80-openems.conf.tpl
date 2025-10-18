server {
	listen 80 default_server;
	listen [::]:80 default_server;

	server_name _;

	root /var/www/html/openems;

	index	index.html;

	# OpenEMS Web-Interface
	location / {
		try_files \$uri \$uri/ /index.html;
		error_page	404 300 /index.html;
	}

	# OpenEMS Edge Proxy
	location /openems-edge {
		proxy_pass http://$WEBSOCKET_HOST:$WEBSOCKET_PORT;
		proxy_http_version 1.1;
		proxy_set_header Upgrade \$http_upgrade;
		proxy_set_header Connection 'upgrade';
		proxy_set_header Host \$host;
		proxy_cache_bypass \$http_upgrade;
	}

	location /rest {
		proxy_pass http://$WEBSOCKET_HOST:$REST_PORT/rest;
		proxy_set_header Host \$host;
		proxy_set_header X-Real-IP \$remote_addr;
		proxy_set_header X-Forwarded-For \$proxy_add_x_forwarded_for;
		proxy_set_header X-Forwarded-Proto http;
	}
}