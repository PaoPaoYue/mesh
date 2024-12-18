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
	prop.PacketMaxSize = getIntFromMap(m, "mesh.rpc.packet_max_size", prop.PacketMaxSize)
	prop.KeepAliveTimeout = getIntFromMap(m, "mesh.rpc.keep_alive_timeout", prop.KeepAliveTimeout)
	prop.KeepAliveInterval = getIntFromMap(m, "mesh.rpc.keep_alive_interval", prop.KeepAliveInterval)
	prop.KeepAliveIdleTimeout = getIntFromMap(m, "mesh.rpc.keep_alive_idle_timeout", prop.KeepAliveIdleTimeout)
	prop.UpstreamMaxResend = getIntFromMap(m, "mesh.rpc.upstream_max_resend", prop.UpstreamMaxResend)
	prop.UpstreamConnectionTimeout = getIntFromMap(m, "mesh.rpc.upstream_connection_timeout", prop.UpstreamConnectionTimeout)
	prop.BlockListSize = getIntFromMap(m, "mesh.rpc.block_list_size", prop.BlockListSize)
	prop.BlockExpireTime = getIntFromMap(m, "mesh.rpc.block_expire_time", prop.BlockExpireTime)
	prop.DiscoveryType = getStringFromMap(m, "mesh.rpc.discovery_type", prop.DiscoveryType)
	prop.MetricsType = getStringFromMap(m, "mesh.rpc.metrics_type", prop.MetricsType)

	// discoveryType must be one of "static" or "k8s"
	if prop.DiscoveryType != StaticDiscovery && prop.DiscoveryType != K8sDiscovery {
		slog.Error("Invalid discovery type", "type", prop.DiscoveryType)
		return errors.New("invalid discovery type")
	}

	// metricsType must be one of "none" or "dogstatsd"
	if prop.MetricsType != NoneMetric && prop.MetricsType != DogStatsDMetric {
		slog.Error("Invalid metrics type", "type", prop.MetricsType)
		return errors.New("invalid metrics type")
	}

	if prop.DiscoveryType == StaticDiscovery {
		if _, ok := m["mesh.rpc.static_services"].([]any); !ok {
			slog.Error("Invalid static services configuration", "services", m["mesh.rpc.static_services"])
			return errors.New("invalid static services")
		}
		for _, service := range m["mesh.rpc.static_services"].([]any) {
			if service, ok := service.(map[string]any); !ok {
				slog.Error("Invalid static services configuration", "services", m["mesh.rpc.static_services"])
				return errors.New("invalid static service")
			} else {
				staticService := discovery.StaticService{
					Name: getStringFromMap(service, "name", ""),
					Env:  getStringFromMap(service, "env", ""),
					Host: getStringFromMap(service, "host", ""),
					Port: int32(getIntFromMap(service, "port", 0)),
				}
				if staticService.Name == "" ||
					staticService.Env == "" ||
					staticService.Host == "" ||
					staticService.Port == 0 {
					slog.Error("Invalid static service configuration", "service", m["mesh.rpc.static_services"])
					return errors.New("invalid static service")
				}
				prop.StaticServices = append(prop.StaticServices, staticService)
			}
		}
	}

	if prop.MetricsType == DogStatsDMetric {
		if metricsEndpoint, ok := m["mesh.rpc.metrics_endpoint"].(map[string]any); ok {
			prop.MetricsEndpoint.Host = getStringFromMap(metricsEndpoint, "host", "")
			prop.MetricsEndpoint.Port = int32(getIntFromMap(metricsEndpoint, "port", 0))
			if prop.MetricsEndpoint.Host == "" || prop.MetricsEndpoint.Port == 0 {
				slog.Error("Invalid metrics endpoint", "endpoint", m["mesh.rpc.metrics_endpoint"])
				return errors.New("invalid metrics endpoint")
			}
		} else {
			slog.Error("Invalid metrics endpoint", "endpoint", m["mesh.rpc.metrics_endpoint"])
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
