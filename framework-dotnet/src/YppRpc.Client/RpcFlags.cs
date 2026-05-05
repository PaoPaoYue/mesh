namespace YppRpc.Client;

public static class RpcFlags
{
    public const int SystemCall = 1 << 7;
    public const int ServiceCall = 1 << 6;
    public const int KeepAlive = 1;
    public const int Fin = 1 << 1;
    public const int ProxyFailed = 1 << 2;
}

