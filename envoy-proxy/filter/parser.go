package filter

import (
	"github.com/paopaoyue/mesh/envoy-proxy/proto"
	pb "google.golang.org/protobuf/proto"
)

type StreamParser struct {
	buf []byte
}

func NewStreamParser() *StreamParser {
	return &StreamParser{
		buf: make([]byte, 0),
	}
}

func (p *StreamParser) Parse(data []byte) ([]*proto.Packet, error) {
	var packets []*proto.Packet
	if len(p.buf) > 0 {
		p.buf = append(p.buf, data...)
		data = p.buf
	}
	for {
		if len(data) < 4 {
			break
		}
		pLen := int32(data[0])<<24 | int32(data[1])<<16 | int32(data[2])<<8 | int32(data[3])
		if len(data) < int(pLen) {
			break
		}
		var packet proto.Packet
		err := pb.Unmarshal(data[:pLen], &packet)
		if err != nil {
			return packets, err
		}
		packets = append(packets, &packet)
		data = data[pLen:]
	}
	p.buf = data
	return packets, nil
}
