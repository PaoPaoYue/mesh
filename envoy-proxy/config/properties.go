package config

import (
	v3 "github.com/cncf/xds/go/xds/type/v3"
	"github.com/go-playground/validator/v10"
	"log/slog"
)

type Properties struct {
	PacketMaxSize        int `validate:"min=1,max=2097152"` // 2 * 1024 * 1024
	KeepAliveTimeout     int `validate:"min=3,max=10"`
	KeepAliveInterval    int `validate:"min=1,max=3"`
	KeepAliveIdleTimeout int `validate:"min=10,max=60"`
}

// NewProperties creates a new instance of Properties with default values
func NewProperties() *Properties {
	return &Properties{
		PacketMaxSize:        1024 * 1024, // Default: 1MB
		KeepAliveTimeout:     6,           // Default: 6
		KeepAliveInterval:    2,           // Default: 2
		KeepAliveIdleTimeout: 10,          // Default: 10
	}
}

// LoadFromConfig loads the properties from the TypedStruct
func (p *Properties) LoadFromConfig(configStruct *v3.TypedStruct) error {
	m := configStruct.Value.AsMap()
	p.PacketMaxSize = getIntFromMap(m, "mesh.rpc.PacketMaxSize", p.PacketMaxSize)
	p.KeepAliveTimeout = getIntFromMap(m, "mesh.rpc.KeepAliveTimeout", p.KeepAliveTimeout)
	p.KeepAliveInterval = getIntFromMap(m, "mesh.rpc.KeepAliveInterval", p.KeepAliveInterval)
	p.KeepAliveIdleTimeout = getIntFromMap(m, "mesh.rpc.KeepAliveIdleTimeout", p.KeepAliveIdleTimeout)

	validate := validator.New()
	err := validate.Struct(p)
	if err != nil {
		slog.Error("Properties validation failed: %v", err)
		return err
	}

	return nil
}

// Helper function to get environment variables as integers with default value
func getIntFromMap(m map[string]any, name string, defaultValue int) int {
	if v, ok := m[name]; ok {
		if value, ok := v.(int); ok {
			return value
		}
	}
	return defaultValue
}
