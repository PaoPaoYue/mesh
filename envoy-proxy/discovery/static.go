package discovery

import "context"

type StaticServiceDiscovery struct {
	BaseServiceDiscovery
}

func NewStaticServiceDiscovery(serviceName, env string, endpoints []Endpoint) *StaticServiceDiscovery {
	sd := &StaticServiceDiscovery{
		BaseServiceDiscovery: NewBaseServiceDiscovery(),
	}
	for _, ep := range endpoints {
		sd.addEndpoint(context.Background(), serviceName, env, ep)
	}
	return sd
}
