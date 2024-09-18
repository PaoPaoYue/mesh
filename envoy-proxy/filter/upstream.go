package filter

import (
	"github.com/envoyproxy/envoy/contrib/golang/common/go/api"
	"github.com/paopaoyue/mesh/envoy-proxy/discovery"
	"github.com/paopaoyue/mesh/envoy-proxy/proto"
	"github.com/paopaoyue/mesh/envoy-proxy/util"
	pb "google.golang.org/protobuf/proto"
	"log/slog"
	"sync"
	"time"
)

type Request struct {
	packet     *proto.Packet
	downFilter *DownFilter
	resend     int // times redirected back to downFilter
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
	slog.Debug("upFilter NewUpFilter", "upFilter", ep)
	return &UpFilter{
		ff:         ff,
		ep:         ep,
		ch:         make(chan Request),
		parser:     NewStreamParser(ff.Prop.PacketMaxSize),
		lastActive: time.Now(),
	}
}

func (f *UpFilter) SendRequest(packet *proto.Packet, downFilter *DownFilter, resend int) {
	f.lock.RLock()
	defer f.lock.RUnlock()
	if !f.closed {
		// save the request id and downFilter, ignore proxy ping request which has no downFilter
		if downFilter != nil {
			f.requests.Store(packet.Header.RequestId, downFilter)
		}
		select {
		case f.ch <- Request{
			packet:     packet,
			downFilter: downFilter,
			resend:     resend,
		}:
		case <-time.After(time.Duration(f.ff.Prop.UpstreamConnectionTimeout) * time.Millisecond):
			slog.Warn("upFilter SendRequest, connection timeout", "upFilter", f.ep, "packet", packet)
		}
	} else {
		slog.Warn("upFilter SendRequest, connection closed and resending", "upFilter", f.ep, "packet", packet)
		if util.IsServiceCall(packet.Header.Flag) {
			downFilter.SendRequest(packet, resend+1)
		}
	}
}

func (f *UpFilter) Close(now bool) {
	f.lock.Lock()
	if !f.closed {
		slog.Debug("upFilter closing", "upFilter", f.ep)
		f.ff.UpFilters.CompareAndDelete(f.ep, f)
		close(f.ch)
		f.closed = true
		f.lock.Unlock()
		if !now {
			data, err := pb.Marshal(util.FinRequestPacket)
			if err != nil {
				slog.Error("upFilter marshal fin request packet error, skipping", "upFilter", f.ep, "error", err.Error())
			} else {
				f.parser.InjectPacketLength(data) // inject length field for dummy response
				slog.Debug("upFilter sending fin request", "upFilter", f.ep)
				f.cb.Write(data, true)
			}
		}
	} else {
		f.lock.Unlock()
	}
}

func (f *UpFilter) CheckActive(duration time.Duration) {
	if f.closed {
		return
	}
	if time.Since(f.lastActive) > duration {
		slog.Debug("upFilter inactive, closing", "upFilter", f.ep, "duration", time.Since(f.lastActive))
		go f.Close(false)
	} else {
		go f.SendRequest(util.PingRequestPacket, nil, 0)
	}
}

func (f *UpFilter) OnPoolReady(cb api.ConnectionCallback) {
	f.cb = cb
	f.cb.EnableHalfClose(false)
	localAddr, _ := f.cb.StreamInfo().UpstreamLocalAddress()
	remoteAddr, _ := f.cb.StreamInfo().UpstreamRemoteAddress()
	slog.Debug("upFilter OnPoolReady", "upFilter", f.ep, "localAddr", localAddr, "remoteAddr", remoteAddr)
	go func() {
		defer func() {
			if r := recover(); r != nil {
				slog.Error("upFilter OnPoolReady panic recovered", "upFilter", f.ep, "error", r)
			}
		}()
		for request := range f.ch {
			f.lock.RLock()
			if f.closed && util.IsServiceCall(request.packet.Header.Flag) {
				downFilter, ok := f.requests.Load(request.packet.Header.RequestId)
				if ok {
					slog.Warn("upFilter closed, resending request", "upFilter", f.ep, "packet", request.packet)
					downFilter.(*DownFilter).SendRequest(request.packet, request.resend+1)
				} else {
					slog.Error("upFilter closed, resending request but downFilter not found", "upFilter", f.ep, "packet", request.packet)
				}
			} else {
				if util.IsServiceCall(request.packet.Header.Flag) {
					f.lastActive = time.Now()
				}
				data, err := pb.Marshal(request.packet)
				if err != nil {
					slog.Error("upFilter marshal request packet error, skipping", "upFilter", f.ep, "error", err.Error())
				} else {
					if request.packet.Header.Length <= lenFieldSize {
						f.parser.InjectPacketLength(data) // inject length field for dummy response
					}
					slog.Debug("upFilter request sending", "upFilter", f.ep, "packet", request.packet)
					f.cb.Write(data, false)
				}
			}
			f.lock.RUnlock()
		}
	}()
}

func (f *UpFilter) OnPoolFailure(poolFailureReason api.PoolFailureReason, transportFailureReason string) {
	slog.Error("upFilter OnPoolFailure", "upFilter", f.ep, "poolFailureReason", poolFailureReason, "transportFailureReason", transportFailureReason)
	go func() {
		defer func() {
			if r := recover(); r != nil {
				slog.Error("upFilter OnPoolFailure panic recovered", "upFilter", f.ep, "error", r)
			}
		}()
		for request := range f.ch {
			downFilter, ok := f.requests.Load(request.packet.Header.RequestId)
			if ok {
				slog.Warn("upFilter OnPoolFailure, resending request", "upFilter", f.ep, "request", request.packet.Header.RequestId)
				downFilter.(*DownFilter).SendRequest(request.packet, request.resend+1)
			} else {
				slog.Error("upFilter OnPoolFailure, resending request but downFilter not found", "upFilter", f.ep, "packet", request.packet)
			}
		}
	}()
	f.lock.Lock()
	defer f.lock.Unlock()
	f.ff.UpFilters.CompareAndDelete(f.ep, f)
	f.closed = true
	close(f.ch)
}

func (f *UpFilter) OnData(buffer []byte, endOfStream bool) {
	packets, err := f.parser.Parse(buffer)
	if err != nil {
		slog.Error("upFilter parse stream data error, closing connection", "upFilter", f.ep, "error", err.Error())
		f.cb.Close(api.NoFlush)
		return
	}
	for _, packet := range packets {
		slog.Debug("upFilter received response", "upFilter", f.ep, "packet", packet)
		if util.IsSystemCall(packet.Header.Flag) {
			continue
		}
		downFilter, ok := f.requests.Load(packet.Header.RequestId)
		if ok {
			f.requests.Delete(packet.Header.RequestId)
			downFilter.(*DownFilter).SendResponse(packet)
		} else {
			slog.Error("upFilter sending response but downFilter not found", "upFilter", f.ep, "packet", packet)
		}
	}

}

func (f *UpFilter) OnEvent(event api.ConnectionEvent) {
	remoteAddr, _ := f.cb.StreamInfo().UpstreamRemoteAddress()
	slog.Debug("upFilter OnEvent", "upFilter", f.ep, "event", event, "remoteAddr", remoteAddr)
	if event == api.LocalClose || event == api.RemoteClose {
		f.Close(true)
	}
}
