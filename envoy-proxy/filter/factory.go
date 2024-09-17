package filter

import (
	"github.com/envoyproxy/envoy/contrib/golang/common/go/api"
	"github.com/envoyproxy/envoy/contrib/golang/filters/network/source/go/pkg/network"
	"github.com/paopaoyue/mesh/envoy-proxy/config"
	"github.com/paopaoyue/mesh/envoy-proxy/discovery"
	"log/slog"
	"sync"
	"time"
)

type StreamFilterFactory struct {
	Prop *config.Properties

	Discovery discovery.ServiceDiscovery

	lock        sync.Mutex
	UpFilters   sync.Map
	DownFilters sync.Map
}

func NewStreamFilterFactory(prop *config.Properties) *StreamFilterFactory {
	return &StreamFilterFactory{
		Prop: prop,
	}
}

func (ff *StreamFilterFactory) CreateFilter(cb api.ConnectionCallback) api.DownstreamFilter {
	return ff.CreateDownFilter(cb)
}

func (ff *StreamFilterFactory) CreateDownFilter(cb api.ConnectionCallback) *DownFilter {
	f := NewDownFilter(ff, cb)
	ff.DownFilters.Store(f.ep, f)
	return f
}

func (ff *StreamFilterFactory) CreateOrGetUpFilter(ep discovery.Endpoint) *UpFilter {
	upFilter, ok := ff.UpFilters.Load(ep)
	if !ok || upFilter.(*UpFilter).closed {
		ff.lock.Lock()
		defer ff.lock.Unlock()
		upFilter, ok = ff.UpFilters.Load(ep)
		if !ok || upFilter.(*UpFilter).closed {
			upFilter = NewUpFilter(ff, ep)
			network.CreateUpstreamConn(ep.String(), upFilter.(*UpFilter))
			ff.UpFilters.Store(ep, upFilter)
		}
	}
	return upFilter.(*UpFilter)
}

func (ff *StreamFilterFactory) RegisterDiscovery(discovery discovery.ServiceDiscovery) {
	ff.Discovery = discovery
}

func (ff *StreamFilterFactory) StartSentinel() {
	checkTicker := time.NewTicker(time.Duration(ff.Prop.KeepAliveInterval) * time.Second)
	maxAlive := time.Duration(ff.Prop.KeepAliveTimeout) * time.Second
	maxIdle := time.Duration(ff.Prop.KeepAliveIdleTimeout) * time.Second
	go func() {
		// recover from panic
		defer func() {
			if r := recover(); r != nil {
				slog.Error("Sentinel panic recovered", "err", r)
			}
		}()
		for t := range checkTicker.C {
			var downStreamCount, upStreamCount int
			ff.DownFilters.Range(func(key, value interface{}) bool {
				downStreamCount++
				value.(*DownFilter).CheckAlive(maxAlive)
				return true
			})
			ff.UpFilters.Range(func(key, value interface{}) bool {
				upStreamCount++
				value.(*UpFilter).CheckActive(maxIdle)
				return true
			})
			slog.Debug("Sentinel check finished", "time", t, "downStreamCount", downStreamCount, "upStreamCount", upStreamCount)
		}
	}()
}
