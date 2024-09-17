package filter

import (
	"context"
	"github.com/envoyproxy/envoy/contrib/golang/common/go/api"
	"github.com/paopaoyue/mesh/envoy-proxy/discovery"
	"github.com/paopaoyue/mesh/envoy-proxy/proto"
	"github.com/paopaoyue/mesh/envoy-proxy/util"
	pb "google.golang.org/protobuf/proto"
	"log/slog"
	"sync"
	"time"
)

type DownFilter struct {
	api.EmptyDownstreamFilter

	ff        *StreamFilterFactory
	ep        discovery.Endpoint
	cb        api.ConnectionCallback
	lock      sync.RWMutex
	closed    bool
	parser    *StreamParser
	lastAlive time.Time
}

func NewDownFilter(ff *StreamFilterFactory, cb api.ConnectionCallback) *DownFilter {
	addr, ok := cb.StreamInfo().UpstreamRemoteAddress()
	if !ok {
		slog.Error("downFilter NewDownFilter, remote address not found")
		return nil
	}
	ep, ok := discovery.NewEndpoint(addr)
	if !ok {
		slog.Error("downFilter NewDownFilter, invalid remote address")
		return nil
	}
	slog.Debug("downFilter NewDownFilter", "downFilter", ep)
	return &DownFilter{
		ff:        ff,
		ep:        ep,
		cb:        cb,
		parser:    NewStreamParser(ff.Prop.PacketMaxSize),
		lastAlive: time.Now(),
	}
}

func (f *DownFilter) SendRequest(packet *proto.Packet, resend int) {
	slog.Debug("downFilter SendRequest", "downFilter", f.ep, "packet", packet)
	if resend > f.ff.Prop.UpstreamMaxResend {
		slog.Warn("downFilter SendRequest, max resend reached", "downFilter",
			f.ep, "resend", resend, "maxResend", f.ff.Prop.UpstreamMaxResend)
		go f.SendResponse(util.NewServiceNotFoundResponsePacket(packet.Header.RequestId))
		return
	}
	f.lock.RLock()
	if f.closed {
		return
	}
	serviceName, env := packet.Header.Service, packet.Header.Env

	ep, found := f.ff.Discovery.SelectEndpoint(context.Background(), serviceName, env)
	if !found {
		slog.Warn("downFilter SendRequest, but service not found", "downFilter", f.ep, "serviceName", serviceName, "env", env)
		go f.SendResponse(util.NewServiceNotFoundResponsePacket(packet.Header.RequestId))
	} else {
		upFilter := f.ff.CreateOrGetUpFilter(ep)
		go upFilter.SendRequest(packet, f, resend)
	}
	f.lock.RUnlock()
}

func (f *DownFilter) SendResponse(packet *proto.Packet) {
	f.lock.RLock()
	defer f.lock.RUnlock()
	if f.closed {
		return
	}
	data, err := pb.Marshal(packet)
	if err != nil {
		slog.Error("downFilter marshal response packet error", "downFilter", f.ep, "error", err.Error())
		return
	}
	if packet.Header.Length <= lenFieldSize {
		f.parser.InjectPacketLength(data) // inject length field for dummy response
	}
	slog.Debug("downFilter SendResponse", "downFilter", f.ep, "packet", packet)
	f.cb.Write(data, false)
}

func (f *DownFilter) Close() {
	f.lock.Lock()
	defer f.lock.Unlock()
	if !f.closed {
		slog.Debug("downFilter closing", "downFilter", f.ep)
		f.ff.DownFilters.CompareAndDelete(f.ep, f)
		f.closed = true
	}
}

func (f *DownFilter) CheckAlive(duration time.Duration) {
	if time.Since(f.lastAlive) > duration {
		slog.Warn("downFilter no request received, closing", "downFilter", f.ep, "duration", time.Since(f.lastAlive))
		f.cb.Close(api.NoFlush)
	}
}

func (f *DownFilter) OnNewConnection() api.FilterStatus {
	localAddr, _ := f.cb.StreamInfo().UpstreamLocalAddress()
	remoteAddr, _ := f.cb.StreamInfo().UpstreamRemoteAddress()
	slog.Debug("downFilter OnNewConnection", "downFilter", f.ep, "localAddr", localAddr, "remoteAddr", remoteAddr)
	return api.NetworkFilterContinue
}

func (f *DownFilter) OnData(buffer []byte, endOfStream bool) api.FilterStatus {
	packets, err := f.parser.Parse(buffer)
	if err != nil {
		slog.Error("downFilter parse stream data error, closing connection", "downFilter", f.ep, "error", err.Error())
		f.cb.Close(api.NoFlush)
		return api.NetworkFilterStopIteration
	}
	for _, packet := range packets {
		f.lastAlive = time.Now()
		if util.IsServiceCall(packet.Header.Flag) {
			f.SendRequest(packet, 0)
		} else if util.IsSystemCall(packet.Header.Flag) {
			if util.IsFin(packet.Header.Flag) {
				f.SendResponse(util.NewFinResponsePacket(packet.Header.RequestId))
			} else {
				f.SendResponse(util.NewPingResponsePacket(packet.Header.RequestId))
			}
		}
	}
	return api.NetworkFilterContinue
}

func (f *DownFilter) OnEvent(event api.ConnectionEvent) {
	slog.Debug("downFilter OnEvent", "downFilter", f.ep, "event", event)
	if event == api.LocalClose || event == api.RemoteClose {
		f.Close()
	}
}

func (f *DownFilter) OnWrite(buffer []byte, endOfStream bool) api.FilterStatus {
	return api.NetworkFilterContinue
}
