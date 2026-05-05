namespace YppRpc.Client;

public sealed class RpcCallOptions
{
    public static RpcCallOptions Default { get; } = new();

    public string ConnectionTag { get; init; } = "default";

    public bool KeepAlive { get; init; } = true;

    public bool Fin { get; init; }

    public TimeSpan Timeout { get; init; } = TimeSpan.FromSeconds(1);

    public int RetryTimes { get; init; }

    public TimeSpan RetryInterval { get; init; } = TimeSpan.Zero;

    public string? EnvironmentOverride { get; init; }
}

