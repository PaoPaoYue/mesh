package discovery

import (
	"context"
)

type StaticService struct {
	name string
	env  string
	host string
	port int32
}

type StaticServiceDiscovery struct {
	BaseServiceDiscovery
}

func NewStaticServiceDiscovery(services []StaticService) *StaticServiceDiscovery {
	sd := &StaticServiceDiscovery{
		BaseServiceDiscovery: NewBaseServiceDiscovery(),
	}
	for _, service := range services {
		sd.addEndpoint(context.Background(), service.name, service.env, Endpoint{
			Host: service.host,
			Port: int(service.port),
		})
	}
	return sd
}
