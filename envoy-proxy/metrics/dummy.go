package metrics

import "github.com/paopaoyue/mesh/envoy-proxy/proto"

type DummyMetricsClient struct{}

func NewDummyMetricsClient() *DummyMetricsClient {
	return &DummyMetricsClient{}
}

func (client *DummyMetricsClient) IncrRequest(packet *proto.Packet) {}

func (client *DummyMetricsClient) IncrSuccess(packet *proto.Packet) {}

func (client *DummyMetricsClient) IncrFailed(packet *proto.Packet) {}

func (client *DummyMetricsClient) GaugeLatency(packet *proto.Packet, latency float64) {}
