package config

import (
	"errors"
	xds "github.com/cncf/xds/go/xds/type/v3"
	"github.com/go-playground/validator/v10"
	"github.com/paopaoyue/mesh/envoy-proxy/discovery"
	"google.golang.org/protobuf/types/known/anypb"
	"log/slog"
)

type Parser struct{}

func (p *Parser) ParseConfig(raw *anypb.Any) any {
	config := &xds.TypedStruct{}
	_ = raw.UnmarshalTo(config)
	m := config.Value.AsMap()

	prop := NewProperties()
	prop.LogLevel = getStringFromMap(m, "log.level", prop.LogLevel)
	prop.PacketMaxSize = getIntFromMap(m, "mesh.rpc.PacketMaxSize", prop.PacketMaxSize)
	prop.KeepAliveTimeout = getIntFromMap(m, "mesh.rpc.KeepAliveTimeout", prop.KeepAliveTimeout)
	prop.KeepAliveInterval = getIntFromMap(m, "mesh.rpc.KeepAliveInterval", prop.KeepAliveInterval)
	prop.KeepAliveIdleTimeout = getIntFromMap(m, "mesh.rpc.KeepAliveIdleTimeout", prop.KeepAliveIdleTimeout)
	prop.UpstreamMaxResend = getIntFromMap(m, "mesh.rpc.UpstreamMaxResend", prop.UpstreamMaxResend)
	prop.UpstreamConnectionTimeout = getIntFromMap(m, "mesh.rpc.UpstreamConnectionTimeout", prop.UpstreamConnectionTimeout)
	prop.BlockListSize = getIntFromMap(m, "mesh.rpc.BlockListSize", prop.BlockListSize)
	prop.BlockExpireTime = getIntFromMap(m, "mesh.rpc.BlockExpireTime", prop.BlockExpireTime)
	prop.DiscoveryType = getStringFromMap(m, "mesh.rpc.DiscoveryType", prop.DiscoveryType)
	prop.MetricsType = getStringFromMap(m, "mesh.rpc.MetricsType", prop.MetricsType)

	// discoveryType must be one of "Static" or "K8s"
	if prop.DiscoveryType != StaticDiscovery && prop.DiscoveryType != K8sDiscovery {
		slog.Error("Invalid discovery type", "type", prop.DiscoveryType)
		return errors.New("invalid discovery type")
	}

	// metricsType must be one of "None" or "dogstatsd"
	if prop.MetricsType != NoneMetric && prop.MetricsType != DogStatsDMetric {
		slog.Error("Invalid metrics type", "type", prop.MetricsType)
		return errors.New("invalid metrics type")
	}

	if prop.DiscoveryType == StaticDiscovery {
		if staticServices, ok := m["mesh.rpc.static_services"].([]discovery.StaticService); ok {
			prop.StaticServices = staticServices
		} else {
			slog.Error("Invalid static services configuration", "services", prop.StaticServices)
			return errors.New("invalid static services")
		}
	}

	if prop.MetricsType == DogStatsDMetric {
		if metricsEndpoint, ok := m["mesh.rpc.metrics_endpoint"].(discovery.Endpoint); ok {
			prop.MetricsEndpoint = metricsEndpoint
		} else {
			slog.Error("Invalid metrics endpoint", "endpoint", prop.MetricsEndpoint)
			return errors.New("invalid metrics endpoint")
		}
	}

	validate := validator.New()
	err := validate.Struct(p)
	if err != nil {
		slog.Error("Properties validation failed", "err", err.Error())
		return err
	}

	return prop
}

func getStringFromMap(m map[string]any, name string, defaultValue string) string {
	if v, ok := m[name]; ok {
		if value, ok := v.(string); ok {
			return value
		}
	}
	return defaultValue
}

func getIntFromMap(m map[string]any, name string, defaultValue int) int {
	if v, ok := m[name]; ok {
		if value, ok := v.(int); ok {
			return value
		}
	}
	return defaultValue
}
