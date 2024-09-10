package main

import (
	xds "github.com/cncf/xds/go/xds/type/v3"
	"github.com/envoyproxy/envoy/contrib/golang/filters/network/source/go/pkg/network"
	config2 "github.com/paopaoyue/mesh/envoy-proxy/config"
	"github.com/paopaoyue/mesh/envoy-proxy/discovery"
	"github.com/paopaoyue/mesh/envoy-proxy/filter"
	"google.golang.org/protobuf/types/known/anypb"
)

func init() {
	network.RegisterNetworkFilterConfigFactory("ypp-rpc-proxy", cf)
}

var cf = &configFactory{}

type configFactory struct{}

func (f *configFactory) CreateFactoryFromConfig(config interface{}) network.FilterFactory {
	a := config.(*anypb.Any)
	configStruct := &xds.TypedStruct{}
	_ = a.UnmarshalTo(configStruct)

	prop := config2.NewProperties()
	_ = prop.LoadFromConfig(configStruct)

	ff := filter.NewStreamFilterFactory(prop)
	ff.RegisterDiscovery(discovery.NewStaticServiceDiscovery("demo", "default", []discovery.Endpoint{
		{
			Addr: "localhost",
			Port: 8080,
		},
	}))
	ff.StartSentinel()
	return ff
}

func main() {
}
