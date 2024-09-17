package util

func IsSystemCall(flag uint32) bool {
	return (flag & (1 << 7)) != 0
}

func IsServiceCall(flag uint32) bool {
	return (flag & (1 << 6)) != 0
}

func IsKeepAlive(flag uint32) bool {
	return (flag & 1) != 0
}

func IsFin(flag uint32) bool {
	return (flag & (1 << 1)) != 0
}
