package metrics

import (
	"fmt"
	"github.com/DataDog/datadog-go/statsd"
	"github.com/paopaoyue/mesh/envoy-proxy/proto"
)

// DogStatsDClient 封装了 DogStatsD 客户端
type DogStatsDClient struct {
	client *statsd.Client
}

func NewDogStatsDClient(host string, port int) (*DogStatsDClient, error) {
	client, err := statsd.New(fmt.Sprintf("%s:%d", host, port), statsd.WithTags([]string{"mode:proxy"}))
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
