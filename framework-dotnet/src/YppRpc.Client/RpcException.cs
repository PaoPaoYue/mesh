namespace YppRpc.Client;

public class RpcException : Exception
{
    public RpcException(string message) : base(message)
    {
    }

    public RpcException(string message, Exception innerException) : base(message, innerException)
    {
    }
}

