package metrics

import (
	"fmt"
	"github.com/DataDog/datadog-go/statsd"
	"github.com/paopaoyue/mesh/envoy-proxy/discovery"
	"github.com/paopaoyue/mesh/envoy-proxy/proto"
)

type DogStatsDClient struct {
	client *statsd.Client
}

func NewDogStatsDClient(endpoint discovery.Endpoint) (*DogStatsDClient, error) {
	client, err := statsd.New(fmt.Sprintf("%s:%d", endpoint.Host, &endpoint.Port), statsd.WithTags([]string{"mode:proxy"}))
	if err != nil {
		return nil, err
	}
	return &DogStatsDClient{client: client}, nil
}

func (client *DogStatsDClient) IncrRequest(packet *proto.Packet) {
	_ = client.client.Incr(getMetricsNameFromPacket(packet, "request"), getTagsFromPacket(packet), 1)
}

func (client *DogStatsDClient) IncrSuccess(packet *proto.Packet) {
	_ = client.client.Incr(getMetricsNameFromPacket(packet, "success"), getTagsFromPacket(packet), 1)
}

func (client *DogStatsDClient) IncrFailed(packet *proto.Packet) {
	_ = client.client.Incr(getMetricsNameFromPacket(packet, "failed"), getTagsFromPacket(packet), 1)
}

func (client *DogStatsDClient) GaugeLatency(packet *proto.Packet, latency float64) {
	_ = client.client.Gauge(getMetricsNameFromPacket(packet, "latency"), latency, getTagsFromPacket(packet), 1)
}
