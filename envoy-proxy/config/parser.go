package config

import (
	xds "github.com/cncf/xds/go/xds/type/v3"
	"github.com/go-playground/validator/v10"
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

	if staticServices, ok := m["mesh.rpc.static_services"].([]any); ok {
		prop.StaticServices = staticServices
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
