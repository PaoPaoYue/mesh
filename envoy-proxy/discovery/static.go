package discovery

import (
	"context"
)

type StaticService struct {
	Name string
	Env  string
	Host string
	Port int32
}

type StaticServiceDiscovery struct {
	BaseServiceDiscovery
}

func NewStaticServiceDiscovery(services []StaticService) *StaticServiceDiscovery {
	sd := &StaticServiceDiscovery{
		BaseServiceDiscovery: NewBaseServiceDiscovery(),
	}
	for _, service := range services {
		sd.addEndpoint(context.Background(), service.Name, service.Env, Endpoint{
			Host: service.Host,
			Port: service.Port,
		})
	}
	return sd
}
