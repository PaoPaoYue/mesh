package util

import (
	"github.com/google/uuid"
	"math"
)

func generateRequestId() int64 {
	id := uuid.New()
	return int64(id[0])<<56 |
		int64(id[1])<<48 |
		int64(id[2])<<40 |
		int64(id[3])<<32 |
		int64(id[4])<<24 |
		int64(id[5])<<16 |
		int64(id[6])<<8 |
		int64(id[7])&math.MaxInt64
}

func generateTraceId() int64 {
	id := uuid.New()
	return int64(id[0])<<56 |
		int64(id[1])<<48 |
		int64(id[2])<<40 |
		int64(id[3])<<32 |
		int64(id[4])<<24 |
		int64(id[5])<<16 |
		int64(id[6])<<8 |
		int64(id[7])&math.MaxInt64
}
