package util

import (
	"github.com/paopaoyue/mesh/envoy-proxy/proto"
	"google.golang.org/protobuf/types/known/anypb"
)

var pingRequestBody, _ = anypb.New(&proto.PingRequest{
	Message: "HEARTBEAT",
})
var PingRequestPacket = &proto.Packet{
	Header: &proto.PacketHeader{
		Length:    1, // calculated on downFilter.SendResponse
		Flag:      129,
		RequestId: 0,
		Service:   "proxy",
		Handler:   "ping",
		Env:       "default",
	},
	TraceInfo: generateEmptyTraceInfo(),
	Body:      pingRequestBody,
}

var pingResponseBody, _ = anypb.New(&proto.PingResponse{
	Message: "OK",
	Base:    &proto.RespBase{},
})

var finRequestBody, _ = anypb.New(&proto.PingRequest{
	Message: "FIN",
})
var FinRequestPacket = &proto.Packet{
	Header: &proto.PacketHeader{
		Length:    1, // calculated on downFilter.SendResponse
		Flag:      131,
		RequestId: 0,
		Service:   "proxy",
		Handler:   "ping",
		Env:       "default",
	},
	TraceInfo: generateEmptyTraceInfo(),
	Body:      finRequestBody,
}

var finResponseBody, _ = anypb.New(&proto.PingResponse{
	Message: "OK",
	Base:    &proto.RespBase{},
})

func NewPingResponsePacket(requestId int64) *proto.Packet {
	return &proto.Packet{
		Header: &proto.PacketHeader{
			Length:    1, // calculated on downFilter.SendResponse
			Flag:      129,
			RequestId: requestId,
			Service:   "proxy",
			Handler:   "ping",
			Env:       "default",
		},
		TraceInfo: generateEmptyTraceInfo(),
		Body:      pingResponseBody,
	}
}

func NewFinResponsePacket(requestId int64) *proto.Packet {
	return &proto.Packet{
		Header: &proto.PacketHeader{
			Length:    1, // calculated on downFilter.SendResponse
			Flag:      131,
			RequestId: requestId,
			Service:   "proxy",
			Handler:   "ping",
			Env:       "default",
		},
		TraceInfo: generateEmptyTraceInfo(),
		Body:      finResponseBody,
	}
}

func NewServiceNotFoundResponsePacket(requestId int64) *proto.Packet {
	return &proto.Packet{
		Header: &proto.PacketHeader{
			Length:    1, // calculated on downFilter.SendResponse
			Flag:      69,
			RequestId: requestId,
			Service:   "proxy",
			Handler:   "ping",
			Env:       "default",
		},
		TraceInfo: generateEmptyTraceInfo(),
	}
}
