ARG ENVOY_IMAGE="envoyproxy/envoy"
ARG ENVOY_VARIANT="contrib-dev"

# Stage 1: Build the Go plugin (ypp-rpc-go-proxy.so)
FROM golang:1.22.5-bookworm@sha256:af9b40f2b1851be993763b85288f8434af87b5678af04355b1e33ff530b5765f AS golang-base

# Set the working directory
WORKDIR /source

# Set environment variables for Go build
ENV GOFLAGS="-buildvcs=false"

# Copy the source code into the container
COPY . /source

# Build the Go plugin
RUN go build -o /build/ypp-rpc-go-proxy.so -buildmode=c-shared .

# Stage 2: Build the Envoy image and include the Go plugin
FROM ${ENVOY_IMAGE}:${ENVOY_VARIANT} AS envoy-go
ARG ENVOY_CONFIG="envoy.yaml"
ENV ENVOY_CONFIG="${ENVOY_CONFIG}"
ENV DEBIAN_FRONTEND=noninteractive
RUN --mount=type=cache,target=/var/cache/apt,sharing=locked \
    --mount=type=cache,target=/var/lib/apt/lists,sharing=locked \
    rm -f /etc/apt/apt.conf.d/docker-clean \
    && echo 'Binary::apt::APT::Keep-Downloaded-Packages "true";' | tee /etc/apt/apt.conf.d/keep-cache \
    && apt-get -qq update -y \
    && apt-get -qq install --no-install-recommends -y curl
COPY --chmod=777 "${ENVOY_CONFIG}" /etc/envoy/envoy.yaml
CMD ["/usr/local/bin/envoy", "-c", "/etc/envoy/envoy.yaml"]

# Set environment variable to disable cgo checks
ENV GODEBUG="cgocheck=0"

# Copy the Go shared object (ypp-rpc-go-proxy.so) from the previous stage
COPY --from=golang-base /build/ypp-rpc-go-proxy.so /lib/ypp-rpc-go-proxy.so

# Set proper permissions for the Go plugin
RUN chmod 777 /lib/ypp-rpc-go-proxy.so