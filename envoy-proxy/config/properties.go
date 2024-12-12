package config

const (
	NoneMetric = "none"
	AutoMetric = "auto"
)

const (
	StaticDiscovery = "static"
	K8sDiscovery    = "k8s"
)

type Properties struct {
	LogLevel                  string
	PacketMaxSize             int `validate:"min=1,max=2097152"` // 2 * 1024 * 1024
	KeepAliveTimeout          int `validate:"min=3,max=10"`      // seconds
	KeepAliveInterval         int `validate:"min=1,max=3"`       // seconds
	KeepAliveIdleTimeout      int `validate:"min=10,max=60"`     // seconds
	UpstreamMaxResend         int `validate:"min=0,max=3"`       // times
	UpstreamConnectionTimeout int `validate:"min=10,max=500"`    // milliseconds
	ParseErrorLimit           int `validate:"min=0,max=100"`     // times per second
	BlockListSize             int `validate:"min=0"`             // size
	BlockExpireTime           int `validate:"min=0"`             // seconds

	MetricsType    string
	DiscoveryType  string
	StaticServices []any
}

func NewProperties() *Properties {
	return &Properties{
		LogLevel:                  "info",      // Default: info
		PacketMaxSize:             1024 * 1024, // Default: 1MB
		KeepAliveTimeout:          6,           // Default: 6
		KeepAliveInterval:         2,           // Default: 2
		KeepAliveIdleTimeout:      10,          // Default: 10
		UpstreamMaxResend:         1,           // Default: 1
		UpstreamConnectionTimeout: 200,         // Default: 200
		ParseErrorLimit:           3,           // Default: 3
		BlockListSize:             1000,        // Default: 1000
		BlockExpireTime:           600,         // Default: 600
		MetricsType:               NoneMetric,
		DiscoveryType:             StaticDiscovery,
		StaticServices:            []any{},
	}
}
