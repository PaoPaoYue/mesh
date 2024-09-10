package filter

import (
	"github.com/envoyproxy/envoy/contrib/golang/common/go/api"
	"github.com/paopaoyue/mesh/envoy-proxy/discovery"
	"github.com/paopaoyue/mesh/envoy-proxy/proto"
	pb "google.golang.org/protobuf/proto"
	"log/slog"
	"sync"
	"time"
)

type Request struct {
	packet     *proto.Packet
	downFilter *DownFilter
}

type UpFilter struct {
	api.EmptyUpstreamFilter

	ff         *StreamFilterFactory
	ep         discovery.Endpoint
	cb         api.ConnectionCallback
	ch         chan Request
	requests   sync.Map
	lock       sync.RWMutex
	closed     bool
	parser     *StreamParser
	lastActive time.Time
}

func NewUpFilter(ff *StreamFilterFactory, ep discovery.Endpoint) *UpFilter {
	return &UpFilter{
		ff:         ff,
		ep:         ep,
		ch:         make(chan Request),
		parser:     NewStreamParser(),
		lastActive: time.Now(),
	}
}

func (f *UpFilter) SendRequest(packet *proto.Packet, downFilter *DownFilter) bool {
	f.lock.RLock()
	defer f.lock.RUnlock()
	if !f.closed {
		// save the request id and downFilter, ignore proxy ping request which has no downFilter
		if downFilter != nil {
			f.requests.Store(packet.Header.RequestId, downFilter)
		}
		f.ch <- Request{
			packet:     packet,
			downFilter: downFilter,
		}
		return true
	}
	return false
}

func (f *UpFilter) Close() {
	f.lock.Lock()
	defer f.lock.Unlock()
	if !f.closed {
		slog.Debug("upFilter %v closing", f.ep)
		f.ff.UpFilters.Delete(f.ep)
		close(f.ch)
		f.cb.Close(api.FlushWrite)
		f.closed = true
	}
}

func (f *UpFilter) CheckActive(duration time.Duration) {
	if f.closed {
		return
	}
	if time.Since(f.lastActive) > duration {
		slog.Debug("upFilter %v inactive for %v, closing", f.ep, duration)
		f.Close()
	}
}

func (f *UpFilter) OnPoolReady(cb api.ConnectionCallback) {
	f.cb = cb
	f.cb.EnableHalfClose(false)
	localAddr, _ := f.cb.StreamInfo().UpstreamLocalAddress()
	remoteAddr, _ := f.cb.StreamInfo().UpstreamRemoteAddress()
	slog.Debug("upFilter %v OnPoolReady, local: %v, remote: %v", f.ep, localAddr, remoteAddr)
	go func() {
		for request := range f.ch {
			f.lock.RLock()
			if f.closed {
				downFilter, ok := f.requests.Load(request.packet.Header.RequestId)
				if ok {
					downFilter.(*DownFilter).SendRequest(request.packet)
				}
			} else {
				data, err := pb.Marshal(request.packet)
				if err != nil {
					slog.Error("upFilter %v marshal request packet error: %v, skipping", f.ep, err)
				} else {
					f.cb.Write(data, false)
				}
			}
			f.lock.RUnlock()
		}
	}()
}

func (f *UpFilter) OnPoolFailure(poolFailureReason api.PoolFailureReason, transportFailureReason string) {
	slog.Error("upFilter %v OnPoolFailure, reason: %v, transportFailureReason: %v", f.ep, poolFailureReason, transportFailureReason)
	f.ff.UpFilters.Delete(f.ep)
	f.closed = true
	close(f.ch)
	for request := range f.ch {
		downFilter, ok := f.requests.Load(request.packet.Header.RequestId)
		if ok {
			downFilter.(*DownFilter).SendRequest(request.packet)
		}
	}
}

func (f *UpFilter) OnData(buffer []byte, endOfStream bool) {
	slog.Debug("upFilter %v OnData,  buffer: %v, endOfStream: %v", f.ep, string(buffer), endOfStream)
	packets, err := f.parser.Parse(buffer)
	if err != nil {
		slog.Error("upFilter %v parse stream data error: %v, closing connection", f.ep, err)
		f.Close()
		return
	}
	for _, packet := range packets {
		if downFilter, ok := f.requests.Load(packet.Header.RequestId); ok {
			f.lastActive = time.Now()
			f.requests.Delete(packet.Header.RequestId)
			downFilter.(*DownFilter).SendResponse(packet)
		}
	}

}

func (f *UpFilter) OnEvent(event api.ConnectionEvent) {
	remoteAddr, _ := f.cb.StreamInfo().UpstreamRemoteAddress()
	slog.Debug("upFilter %v OnEvent, addr: %v, event: %v", f.ep, remoteAddr, event)
	if event == api.LocalClose || event == api.RemoteClose {
		f.Close()
	}
}
