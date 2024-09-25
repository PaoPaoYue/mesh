package metrics

import "github.com/paopaoyue/mesh/envoy-proxy/proto"

type Client interface {
	IncrRequest(packet *proto.Packet)
	IncrSuccess(packet *proto.Packet)
	IncrFailed(packet *proto.Packet)
	GaugeLatency(packet *proto.Packet, latency float64)
}
