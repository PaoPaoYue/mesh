package util

import (
	"github.com/envoyproxy/envoy/contrib/golang/common/go/api"
	"github.com/paopaoyue/mesh/envoy-proxy/proto"
	"log/slog"
	"os"
	"time"
)

func generateTraceInfo(streamInfo api.StreamInfo) *proto.TraceInfo {
	localAddr, localPort := getLocalPortAndAddress(streamInfo)
	return &proto.TraceInfo{
		TraceId:      generateTraceId(),
		StartTime:    time.Now().UnixMilli(),
		UpperService: "",
		UpperHandler: "",
		UpperEnv:     "default",
		UpperDevice:  getDeviceName(),
		UpperHost:    localAddr,
		UpperPort:    localPort,
	}
}

func generateEmptyTraceInfo() *proto.TraceInfo {
	return &proto.TraceInfo{
		TraceId:      0,
		StartTime:    0,
		UpperService: "",
		UpperHandler: "",
		UpperEnv:     "default",
		UpperDevice:  getDeviceName(),
		UpperHost:    "",
		UpperPort:    0,
	}
}

func getLocalPortAndAddress(streamInfo api.StreamInfo) (string, int32) {
	localAddr, ok := streamInfo.UpstreamLocalAddress()
	if !ok {
		slog.Error("Error retrieving local address")
		return "", 0
	}
	// TODO: Fix this
	return localAddr, 0
}

func getDeviceName() string {
	hostname, err := os.Hostname()
	if err != nil {
		slog.Error("Error retrieving hostname: %v", err)
		return ""
	}
	return hostname
}
