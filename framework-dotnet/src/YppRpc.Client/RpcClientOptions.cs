using System.Collections.ObjectModel;

namespace YppRpc.Client;

public sealed class RpcClientOptions
{
    public IDictionary<string, RpcServiceEndpoint> Services { get; } = new Dictionary<string, RpcServiceEndpoint>(StringComparer.OrdinalIgnoreCase);

    public string Environment { get; set; } = "default";

    // Align with Java `mesh.rpc.clientEnabled` semantics while keeping client-only SDK behavior.
    public bool ClientEnabled { get; set; } = true;

    public RpcServiceEndpoint? DefaultClientService { get; set; }

    public TimeSpan ClientShutdownTimeout { get; set; } = TimeSpan.FromSeconds(10);

    public TimeSpan HeartbeatInterval { get; set; } = TimeSpan.FromSeconds(2);

    public TimeSpan HeartbeatTimeout { get; set; } = TimeSpan.FromSeconds(1);

    public TimeSpan KeepAliveTimeout { get; set; } = TimeSpan.FromSeconds(6);

    public TimeSpan KeepAliveIdleTimeout { get; set; } = TimeSpan.FromSeconds(10);

    public int MaxPacketSizeBytes { get; set; } = 1024 * 1024;

    public IReadOnlyDictionary<string, RpcServiceEndpoint> ReadOnlyServices => new ReadOnlyDictionary<string, RpcServiceEndpoint>(Services);
}

