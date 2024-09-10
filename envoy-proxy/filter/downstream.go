package filter

import (
	"context"
	"github.com/envoyproxy/envoy/contrib/golang/common/go/api"
	"github.com/paopaoyue/mesh/envoy-proxy/discovery"
	"github.com/paopaoyue/mesh/envoy-proxy/proto"
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
	return &DownFilter{
		ff:        ff,
		ep:        discovery.Endpoint{Addr: cb.StreamInfo().DownstreamRemoteAddress()},
		cb:        cb,
		parser:    NewStreamParser(),
		lastAlive: time.Now(),
	}
}

func (f *DownFilter) SendRequest(packet *proto.Packet) {
	f.lock.RLock()
	if f.closed {
		return
	}
	serviceName, env := packet.Header.Service, packet.Header.Env
Loop:
	ep, found := f.ff.Discovery.SelectEndpoint(context.Background(), serviceName, env)
	if !found {
		slog.Warn("downFilter %v SendRequest, service %s in env %s not found", f.ep, serviceName, env)
		defer f.Close()
	} else {
		upFilter := f.ff.CreateOrGetUpFilter(ep)
		ok := upFilter.SendRequest(packet, f)
		if !ok {
			goto Loop
		}
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
		slog.Error("downFilter %v marshal response packet error: %v", f.ep, err)
		return
	}
	f.cb.Write(data, false)
}

func (f *DownFilter) Close() {
	f.lock.Lock()
	defer f.lock.Unlock()
	if !f.closed {
		slog.Debug("downFilter %v closing", f.ep)
		f.ff.DownFilters.Delete(f.ep)
		f.cb.Close(api.FlushWrite)
		f.closed = true
	}
}

func (f *DownFilter) CheckAlive(duration time.Duration) {
	if time.Since(f.lastAlive) > duration {
		slog.Warn("downFilter %v no request received for %v, closing", f.ep, duration)
		f.Close()
	}
}

func (f *DownFilter) OnNewConnection() api.FilterStatus {
	localAddr, _ := f.cb.StreamInfo().UpstreamLocalAddress()
	remoteAddr, _ := f.cb.StreamInfo().UpstreamRemoteAddress()
	slog.Debug("downFilter %v OnNewConnection, local: %v, remote: %v", f.ep, localAddr, remoteAddr)
	return api.NetworkFilterContinue
}

func (f *DownFilter) OnData(buffer []byte, endOfStream bool) api.FilterStatus {
	slog.Debug("downFilter %v OnData, buffer: %v, endOfStream: %v\n", f.ep, string(buffer), endOfStream)
	packets, err := f.parser.Parse(buffer)
	if err != nil {
		slog.Error("downFilter %v parse stream data error: %v, closing connection", f.ep, err)
		f.Close()
		return api.NetworkFilterStopIteration
	}
	for _, packet := range packets {
		f.lastAlive = time.Now()
		f.SendRequest(packet)
	}
	return api.NetworkFilterContinue
}

func (f *DownFilter) OnEvent(event api.ConnectionEvent) {
	slog.Debug("downFilter %v OnEvent, addr: %v, event: %v\n", f.ep, event)
	if event == api.LocalClose || event == api.RemoteClose {
		f.Close()
	}
}

func (f *DownFilter) OnWrite(buffer []byte, endOfStream bool) api.FilterStatus {
	slog.Debug("downFilter %v OnWrite, buffer: %v, endOfStream: %v\n", f.ep, string(buffer), endOfStream)
	return api.NetworkFilterContinue
}
