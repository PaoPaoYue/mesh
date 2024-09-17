package main

import (
	"context"
	"github.com/envoyproxy/envoy/contrib/golang/filters/network/source/go/pkg/network"
	"github.com/paopaoyue/mesh/envoy-proxy/config"
	"github.com/paopaoyue/mesh/envoy-proxy/discovery"
	"github.com/paopaoyue/mesh/envoy-proxy/filter"
	"log/slog"
	"strings"
	"sync"
)

func init() {
	network.RegisterNetworkFilterConfigFactory("ypp-rpc-go-proxy", cf)
	network.RegisterNetworkFilterConfigParser(&config.Parser{})
}

var cf = &configFactory{}

type configFactory struct {
	ff   *filter.StreamFilterFactory
	once sync.Once
}

func (f *configFactory) CreateFactoryFromConfig(any interface{}) network.FilterFactory {
	f.once.Do(func() {
		prop, ok := any.(*config.Properties)
		if !ok {
			panic("invalid config")
		}

		switch strings.ToLower(prop.LogLevel) {
		case "debug":
			slog.SetLogLoggerLevel(slog.LevelDebug)
		case "info":
			slog.SetLogLoggerLevel(slog.LevelInfo)
		case "warn":
			slog.SetLogLoggerLevel(slog.LevelWarn)
		case "error":
			slog.SetLogLoggerLevel(slog.LevelError)
		}

		slog.Info("Initiating YPP RPC Go Proxy Plugin", "properties", prop)

		f.ff = filter.NewStreamFilterFactory(prop)
		if prop.StaticServices != nil {
			f.ff.RegisterDiscovery(discovery.NewStaticServiceDiscovery(prop.StaticServices))
		} else {
			sd := discovery.NewK8sServiceDiscovery()
			sd.Watch(context.Background())
			f.ff.RegisterDiscovery(sd)
		}
		f.ff.StartSentinel()
	})
	return f.ff
}

func main() {
}
