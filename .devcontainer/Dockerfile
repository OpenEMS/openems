FROM mcr.microsoft.com/devcontainers/java:21-jdk-bookworm

# Install Node.js 22
RUN curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

# Install Angular CLI globally
RUN npm install -g @angular/cli

# Disable Angular analytics
ENV NG_CLI_ANALYTICS=false
