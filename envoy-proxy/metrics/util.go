package metrics

import "github.com/paopaoyue/mesh/envoy-proxy/proto"

func getMetricsNameFromPacket(packet *proto.Packet, suffix string) string {
	return packet.Header.GetService() + packet.Header.GetHandler() + "." + suffix
}

func getTagsFromPacket(packet *proto.Packet) []string {
	return []string{
		"env:" + packet.Header.GetEnv(),
		"upper_service:" + packet.TraceInfo.GetUpperService(),
		"upper_handler:" + packet.TraceInfo.GetUpperHandler(),
		"upper_env:" + packet.TraceInfo.GetUpperEnv(),
	}
}
