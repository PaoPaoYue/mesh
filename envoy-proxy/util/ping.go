package util

import (
	"github.com/paopaoyue/mesh/envoy-proxy/proto"
	"google.golang.org/protobuf/types/known/anypb"
)

var pingBody, _ = anypb.New(&proto.PingRequest{
	Message: "HEARTBEAT",
})
var PingPacket = &proto.Packet{
	Header: &proto.PacketHeader{
		Length:    116,
		Flag:      129,
		RequestId: 0,
		Service:   "proxy",
		Handler:   "ping",
		Env:       "default",
	},
	TraceInfo: generateEmptyTraceInfo(),
	Body:      pingBody,
}
