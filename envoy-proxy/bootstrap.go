package main

import (
	"github.com/envoyproxy/envoy/contrib/golang/filters/network/source/go/pkg/network"
	"github.com/hashicorp/golang-lru/v2/expirable"
	"github.com/paopaoyue/mesh/envoy-proxy/config"
	"github.com/paopaoyue/mesh/envoy-proxy/filter"
	"log/slog"
	"strings"
	"sync"
	"time"
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

func (f *configFactory) CreateFactoryFromConfig(c interface{}) network.FilterFactory {
	f.once.Do(func() {
		prop, ok := c.(*config.Properties)
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

		filter.DownstreamBlockList = expirable.NewLRU[string, any](prop.BlockListSize, nil, time.Duration(prop.BlockExpireTime)*time.Second)

		f.ff = filter.NewStreamFilterFactory(prop)
		f.ff.RegisterDiscovery()
		f.ff.RegisterMetrics()
		f.ff.StartSentinel()

	})
	return f.ff
}

func main() {
}
