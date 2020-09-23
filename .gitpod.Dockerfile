FROM gitpod/workspace-full

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
             && sdk install java 8.0.265-open"

# disable angular analytics
ENV NG_CLI_ANALYTICS=false

RUN npm install -g @angular/cli 
