package discovery

import (
	"context"
	"fmt"
	"sync"
	"sync/atomic"
)

type Endpoint struct {
	Addr string
	Port int32
}

func (e *Endpoint) String() string {
	return fmt.Sprintf("%s:%d", e.Addr, e.Port)
}

type EndpointGroup struct {
	endpoints []Endpoint
	index     atomic.Int32 // round-robin index
}

func NewEndpointGroup(endpoints []Endpoint) *EndpointGroup {
	return &EndpointGroup{
		endpoints: endpoints,
	}
}

func (eg *EndpointGroup) next() (Endpoint, bool) {
	if len(eg.endpoints) == 0 {
		return Endpoint{}, false
	}
	index := eg.index.Add(1) % int32(len(eg.endpoints))
	return eg.endpoints[index], true
}

func (eg *EndpointGroup) addEndpoint(endpoint Endpoint) {
	for _, e := range eg.endpoints {
		if e.Addr == endpoint.Addr && e.Port == endpoint.Port {
			return
		}
	}
	eg.endpoints = append(eg.endpoints, endpoint)
}

func (eg *EndpointGroup) removeEndpoint(endpoint Endpoint) {
	for i, e := range eg.endpoints {
		if e.Addr == endpoint.Addr && e.Port == endpoint.Port {
			eg.endpoints = append(eg.endpoints[:i], eg.endpoints[i+1:]...)
			break
		}
	}
}

type BaseServiceDiscovery struct {
	serviceMap map[string]*EndpointGroup
	lock       sync.RWMutex
}

func NewBaseServiceDiscovery() BaseServiceDiscovery {
	return BaseServiceDiscovery{
		serviceMap: make(map[string]*EndpointGroup),
	}
}

func (sd *BaseServiceDiscovery) addEndpoint(ctx context.Context, serviceName, env string, endpoint Endpoint) {
	sd.lock.Lock()
	defer sd.lock.Unlock()
	key := getEndpointGroupKey(serviceName, env)
	if eg, ok := sd.serviceMap[key]; !ok {
		sd.serviceMap[key] = NewEndpointGroup([]Endpoint{endpoint})
	} else {
		eg.addEndpoint(endpoint)
	}
}

func (sd *BaseServiceDiscovery) removeEndpoint(ctx context.Context, serviceName, env string, endpoint Endpoint) {
	sd.lock.Lock()
	defer sd.lock.Unlock()
	key := getEndpointGroupKey(serviceName, env)
	if eg, ok := sd.serviceMap[key]; ok {
		eg.removeEndpoint(endpoint)
	}
	if len(sd.serviceMap[key].endpoints) == 0 {
		delete(sd.serviceMap, key)
	}
}

func (sd *BaseServiceDiscovery) listEndpoints(ctx context.Context, serviceName, env string) []Endpoint {
	sd.lock.RLock()
	defer sd.lock.RUnlock()
	key := getEndpointGroupKey(serviceName, env)
	if eg, ok := sd.serviceMap[key]; ok {
		return eg.endpoints
	}
	return []Endpoint{}
}

func (sd *BaseServiceDiscovery) SelectEndpoint(ctx context.Context, serviceName, env string) (Endpoint, bool) {
	sd.lock.RLock()
	defer sd.lock.RUnlock()
	key := getEndpointGroupKey(serviceName, env)
	if eg, ok := sd.serviceMap[key]; ok {
		return eg.next()
	}
	return Endpoint{}, false
}

func getEndpointGroupKey(serviceName, env string) string {
	return fmt.Sprintf("%s|%s", serviceName, env)
}