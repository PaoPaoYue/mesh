package filter

import (
	"errors"
	"github.com/paopaoyue/mesh/envoy-proxy/proto"
	"github.com/paopaoyue/mesh/envoy-proxy/util"
	pb "google.golang.org/protobuf/proto"
	"log/slog"
)

const (
	lenFieldSize   = 4
	lenFieldOffset = 3
)

type StreamParser struct {
	buf     []byte
	maxSize int
}

func NewStreamParser(maxSize int) *StreamParser {
	return &StreamParser{
		buf:     make([]byte, 0),
		maxSize: maxSize,
	}
}

func (p *StreamParser) Parse(data []byte) ([]*proto.Packet, error) {
	var packets []*proto.Packet
	if len(p.buf) > 0 {
		p.buf = append(p.buf, data...)
		data = p.buf
	}
	for {
		if len(data) < lenFieldSize+lenFieldOffset {
			break
		}
		var pLen uint32
		for i := 0; i < lenFieldSize; i++ {
			pLen += uint32(data[i+lenFieldOffset]) << (i * 8)
		}
		if pLen >= uint32(p.maxSize) {
			slog.Warn("StreamParser Parse, packet size exceeds the limit", "pLen", pLen, "maxSize", p.maxSize)
			return packets, errors.New("packet size exceeds the limit")
		}
		if len(data) < int(pLen) || pLen == 0 {
			break
		}
		var packet proto.Packet
		err := pb.Unmarshal(data[:pLen], &packet)
		if err != nil {
			slog.Warn("StreamParser Parse, unmarshal packet failed", "err", err.Error())
			return packets, err
		}
		if !util.IsSystemCall(packet.Header.Flag) && !util.IsServiceCall(packet.Header.Flag) {
			slog.Warn("StreamParser Parse, invalid packet flag", "flag", packet.Header.Flag)
			return packets, errors.New("invalid packet flag")
		}
		packets = append(packets, &packet)
		data = data[pLen:]
	}
	p.buf = data
	return packets, nil
}

func (p *StreamParser) InjectPacketLength(data []byte) {
	pLen := len(data)
	for i := 0; i < lenFieldSize; i++ {
		data[i+lenFieldOffset] = byte(pLen & 0xff)
		pLen >>= 8
	}
}
