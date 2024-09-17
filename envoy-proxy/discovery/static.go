package discovery

import (
	"context"
)

type StaticServiceDiscovery struct {
	BaseServiceDiscovery
}

func NewStaticServiceDiscovery(services any) *StaticServiceDiscovery {
	sd := &StaticServiceDiscovery{
		BaseServiceDiscovery: NewBaseServiceDiscovery(),
	}
	for _, service := range services.([]any) {
		info := service.(map[string]any)
		serviceName := info["name"].(string)
		env := info["env"].(string)
		host := info["host"].(string)
		port := int32(info["port"].(float64))
		sd.addEndpoint(context.Background(), serviceName, env, Endpoint{
			Addr: host,
			Port: port,
		})
	}
	return sd
}
