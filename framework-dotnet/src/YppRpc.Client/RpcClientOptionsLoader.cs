using System.Text.Json;

namespace YppRpc.Client;

public static class RpcClientOptionsLoader
{
    private const string DefaultSectionName = "YppRpc";

    public static RpcClientOptions LoadFromFile(string filePath, string sectionName = "YppRpc")
    {
        if (string.IsNullOrWhiteSpace(filePath))
        {
            throw new ArgumentException("File path is required", nameof(filePath));
        }

        if (!File.Exists(filePath))
        {
            throw new FileNotFoundException("Rpc client config file was not found", filePath);
        }

        var json = File.ReadAllText(filePath);
        return LoadFromJson(json, sectionName);
    }

    public static RpcClientOptions LoadFromJson(string json, string sectionName = "YppRpc")
    {
        if (string.IsNullOrWhiteSpace(json))
        {
            throw new ArgumentException("Config json is required", nameof(json));
        }

        if (string.IsNullOrWhiteSpace(sectionName))
        {
            throw new ArgumentException("Section name is required", nameof(sectionName));
        }

        using var document = JsonDocument.Parse(json);
        if (!TryGetSection(document.RootElement, sectionName, out var section) || section.ValueKind != JsonValueKind.Object)
        {
            throw new InvalidOperationException($"Config section '{sectionName}' was not found.");
        }

        var model = section.Deserialize<RpcClientOptionsModel>(new JsonSerializerOptions
        {
            PropertyNameCaseInsensitive = true
        }) ?? new RpcClientOptionsModel();

        var options = new RpcClientOptions
        {
            Environment = string.IsNullOrWhiteSpace(model.Environment)
                ? (string.IsNullOrWhiteSpace(model.Env) ? "default" : model.Env)
                : model.Environment,
            ClientEnabled = model.ClientEnabled ?? true,
            ClientShutdownTimeout = SecondsOrDefault(model.ClientShutdownTimeout, TimeSpan.FromSeconds(10)),
            HeartbeatInterval = model.HeartbeatInterval ?? SecondsOrDefault(model.KeepAliveInterval, TimeSpan.FromSeconds(2)),
            HeartbeatTimeout = model.HeartbeatTimeout ?? SecondsOrDefault(model.KeepAliveHeartbeatTimeout, TimeSpan.FromSeconds(1)),
            KeepAliveTimeout = SecondsOrDefault(model.KeepAliveTimeout, TimeSpan.FromSeconds(6)),
            KeepAliveIdleTimeout = SecondsOrDefault(model.KeepAliveIdleTimeout, TimeSpan.FromSeconds(10)),
            MaxPacketSizeBytes = model.MaxPacketSizeBytes ?? model.PacketMaxSize ?? 1024 * 1024
        };

        if (TryBuildEndpoint(model.DefaultClientService, out var defaultServiceName, out var defaultEndpoint))
        {
            options.DefaultClientService = defaultEndpoint;
            options.Services[defaultServiceName] = defaultEndpoint;
        }

        if (model.ClientServices is not null)
        {
            foreach (var service in model.ClientServices)
            {
                if (!TryBuildEndpoint(service, out var serviceName, out var endpoint))
                {
                    continue;
                }

                options.Services[serviceName] = endpoint;
            }
        }

        if (model.Services is not null)
        {
            foreach (var pair in model.Services)
            {
                if (string.IsNullOrWhiteSpace(pair.Key))
                {
                    continue;
                }

                options.Services[pair.Key] = pair.Value;
            }
        }

        return options;
    }

    private static bool TryGetSection(JsonElement root, string sectionName, out JsonElement section)
    {
        if (TryGetByPath(root, sectionName, out section))
        {
            return true;
        }

        if (string.Equals(sectionName, DefaultSectionName, StringComparison.OrdinalIgnoreCase))
        {
            if (TryGetByPath(root, "Mesh:Rpc", out section))
            {
                return true;
            }

            if (TryGetByPath(root, "mesh.rpc", out section))
            {
                return true;
            }
        }

        section = default;
        return false;
    }

    private static bool TryGetByPath(JsonElement root, string path, out JsonElement section)
    {
        var segments = path.Split(new[] { ':', '.' }, StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        if (segments.Length == 0)
        {
            section = default;
            return false;
        }

        var current = root;
        foreach (var segment in segments)
        {
            if (!TryGetPropertyIgnoreCase(current, segment, out var next))
            {
                section = default;
                return false;
            }

            current = next;
        }

        section = current;
        return true;
    }

    private static bool TryBuildEndpoint(RpcServiceModel? model, out string serviceName, out RpcServiceEndpoint endpoint)
    {
        serviceName = string.Empty;
        endpoint = default!;

        if (model is null || string.IsNullOrWhiteSpace(model.Name) || string.IsNullOrWhiteSpace(model.Host) || model.Port is null)
        {
            return false;
        }

        serviceName = model.Name;
        endpoint = new RpcServiceEndpoint(model.Host, model.Port.Value);
        return true;
    }

    private static TimeSpan SecondsOrDefault(int? seconds, TimeSpan fallback)
    {
        if (seconds is null || seconds.Value <= 0)
        {
            return fallback;
        }

        return TimeSpan.FromSeconds(seconds.Value);
    }

    private static bool TryGetPropertyIgnoreCase(JsonElement element, string propertyName, out JsonElement property)
    {
        foreach (var current in element.EnumerateObject())
        {
            if (string.Equals(current.Name, propertyName, StringComparison.OrdinalIgnoreCase))
            {
                property = current.Value;
                return true;
            }
        }

        property = default;
        return false;
    }

    private sealed class RpcClientOptionsModel
    {
        public Dictionary<string, RpcServiceEndpoint>? Services { get; set; }

        public string? Environment { get; set; }

        public string? Env { get; set; }

        public bool? ClientEnabled { get; set; }

        public RpcServiceModel? DefaultClientService { get; set; }

        public List<RpcServiceModel>? ClientServices { get; set; }

        public int? ClientShutdownTimeout { get; set; }

        public int? PacketMaxSize { get; set; }

        public int? KeepAliveTimeout { get; set; }

        public int? KeepAliveInterval { get; set; }

        public int? KeepAliveIdleTimeout { get; set; }

        public int? KeepAliveHeartbeatTimeout { get; set; }

        public TimeSpan? HeartbeatInterval { get; set; }

        public TimeSpan? HeartbeatTimeout { get; set; }

        public int? MaxPacketSizeBytes { get; set; }
    }

    private sealed class RpcServiceModel
    {
        public string? Name { get; set; }

        public string? Host { get; set; }

        public int? Port { get; set; }
    }
}

