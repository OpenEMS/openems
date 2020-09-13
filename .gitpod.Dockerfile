FROM gitpod/workspace-full

RUN bash -c ". /home/gitpod/.sdkman/bin/sdkman-init.sh \
             && sdk install java 8.0.265-open"

ENV INVALIDATE_CACHE=1

RUN npm install -g @angular/cli 
