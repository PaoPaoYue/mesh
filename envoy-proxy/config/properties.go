package config

type Properties struct {
	LogLevel                  string
	PacketMaxSize             int `validate:"min=1,max=2097152"` // 2 * 1024 * 1024
	KeepAliveTimeout          int `validate:"min=3,max=10"`      // seconds
	KeepAliveInterval         int `validate:"min=1,max=3"`       // seconds
	KeepAliveIdleTimeout      int `validate:"min=10,max=60"`     // seconds
	UpstreamMaxResend         int `validate:"min=0,max=3"`       // times
	UpstreamConnectionTimeout int `validate:"min=10,max=500"`    // milliseconds
	StaticServices            []any
}

func NewProperties() *Properties {
	return &Properties{
		LogLevel:                  "info",      // Default: info
		PacketMaxSize:             1024 * 1024, // Default: 1MB
		KeepAliveTimeout:          6,           // Default: 6
		KeepAliveInterval:         2,           // Default: 2
		KeepAliveIdleTimeout:      10,          // Default: 10
		UpstreamMaxResend:         1,           // Default: 1
		UpstreamConnectionTimeout: 10,          // Default: 10
	}
}
