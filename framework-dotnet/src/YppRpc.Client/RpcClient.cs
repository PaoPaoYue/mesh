using System.Collections.Concurrent;
using System.Net.Sockets;
using Google.Protobuf;
using Google.Protobuf.WellKnownTypes;
using Rpc;

namespace YppRpc.Client;

public sealed class RpcClient : IAsyncDisposable
{
    private readonly RpcClientOptions _options;
    private readonly ConcurrentDictionary<string, RpcConnection> _keepAliveConnections = new(StringComparer.OrdinalIgnoreCase);
    private readonly SemaphoreSlim _poolLock = new(1, 1);
    private Timer? _heartbeatTimer;
    private long _requestId;

    public RpcClient(RpcClientOptions options)
    {
        _options = options;
    }

    public Task StartAsync(CancellationToken cancellationToken = default)
    {
        if (_heartbeatTimer is not null)
        {
            return Task.CompletedTask;
        }

        _heartbeatTimer = new Timer(async _ =>
        {
            try
            {
                await HeartbeatTickAsync().ConfigureAwait(false);
            }
            catch
            {
                // Keep timer alive; broken connections are removed in heartbeat loop.
            }
        }, null, _options.HeartbeatInterval, _options.HeartbeatInterval);
        return Task.CompletedTask;
    }

    public async Task<TResponse> CallAsync<TRequest, TResponse>(
        string serviceName,
        string handlerName,
        TRequest request,
        bool isSystemCall = false,
        RpcCallOptions? callOptions = null,
        CancellationToken cancellationToken = default)
        where TRequest : class, IMessage
        where TResponse : class, IMessage<TResponse>, new()
    {
        if (string.IsNullOrWhiteSpace(serviceName))
        {
            throw new ArgumentException("Service name is required", nameof(serviceName));
        }

        if (string.IsNullOrWhiteSpace(handlerName))
        {
            throw new ArgumentException("Handler name is required", nameof(handlerName));
        }

        callOptions ??= RpcCallOptions.Default;
        var finalError = default(Exception);

        for (var attempt = 0; attempt <= callOptions.RetryTimes; attempt++)
        {
            var connection = default(RpcConnection);
            var removeKey = string.Empty;
            try
            {
                (connection, removeKey) = await GetConnectionAsync(serviceName, callOptions.ConnectionTag, callOptions.KeepAlive, cancellationToken).ConfigureAwait(false);
                var packet = BuildPacket(serviceName, handlerName, request, isSystemCall, callOptions);
                var response = await connection.SendAsync(packet, callOptions.Timeout, cancellationToken).ConfigureAwait(false);

                if ((response.Header.Flag & RpcFlags.ProxyFailed) != 0)
                {
                    throw new RpcException("Service not found at proxy");
                }

                if (!callOptions.KeepAlive)
                {
                    await connection.DisposeAsync().ConfigureAwait(false);
                }

                return response.Body.Unpack<TResponse>();
            }
            catch (Exception ex)
            {
                finalError = ex;

                if (!string.IsNullOrEmpty(removeKey) && _keepAliveConnections.TryRemove(removeKey, out var stale))
                {
                    await stale.DisposeAsync().ConfigureAwait(false);
                }

                if (attempt == callOptions.RetryTimes)
                {
                    break;
                }

                if (callOptions.RetryInterval > TimeSpan.Zero)
                {
                    await Task.Delay(callOptions.RetryInterval, cancellationToken).ConfigureAwait(false);
                }
            }
        }

        throw new RpcException("RPC call failed", finalError ?? new Exception("Unknown failure"));
    }

    public async ValueTask DisposeAsync()
    {
        _heartbeatTimer?.Dispose();
        _heartbeatTimer = null;

        foreach (var pair in _keepAliveConnections)
        {
            await pair.Value.DisposeAsync().ConfigureAwait(false);
        }

        _keepAliveConnections.Clear();
    }

    private async Task<(RpcConnection connection, string keepAliveKey)> GetConnectionAsync(string serviceName, string tag, bool keepAlive, CancellationToken cancellationToken)
    {
        if (!_options.Services.TryGetValue(serviceName, out var endpoint))
        {
            throw new RpcException($"Service endpoint is not configured: {serviceName}");
        }

        if (!keepAlive)
        {
            var temporary = new RpcConnection(
                serviceName,
                endpoint.Host,
                endpoint.Port,
                _options.MaxPacketSizeBytes,
                keepAlive: false,
                _options.Environment,
                () => Interlocked.Increment(ref _requestId),
                _options.ClientShutdownTimeout);
            await temporary.EnsureConnectedAsync(cancellationToken).ConfigureAwait(false);
            return (temporary, string.Empty);
        }

        var key = serviceName + "|" + tag;

        await _poolLock.WaitAsync(cancellationToken).ConfigureAwait(false);
        try
        {
            if (!_keepAliveConnections.TryGetValue(key, out var current) || !current.IsConnected)
            {
                if (current is not null)
                {
                    await current.DisposeAsync().ConfigureAwait(false);
                }

                current = new RpcConnection(
                    serviceName,
                    endpoint.Host,
                    endpoint.Port,
                    _options.MaxPacketSizeBytes,
                    keepAlive: true,
                    _options.Environment,
                    () => Interlocked.Increment(ref _requestId),
                    _options.ClientShutdownTimeout);
                _keepAliveConnections[key] = current;
            }
            else if (current.IsIdleTimedOut(_options.KeepAliveIdleTimeout))
            {
                await current.DisposeAsync().ConfigureAwait(false);
                current = new RpcConnection(
                    serviceName,
                    endpoint.Host,
                    endpoint.Port,
                    _options.MaxPacketSizeBytes,
                    keepAlive: true,
                    _options.Environment,
                    () => Interlocked.Increment(ref _requestId),
                    _options.ClientShutdownTimeout);
                _keepAliveConnections[key] = current;
            }

            await current.EnsureConnectedAsync(cancellationToken).ConfigureAwait(false);
            return (current, key);
        }
        finally
        {
            _poolLock.Release();
        }
    }

    private Packet BuildPacket<TRequest>(string serviceName, string handlerName, TRequest request, bool isSystemCall, RpcCallOptions callOptions)
        where TRequest : class, IMessage
    {
        var requestId = Interlocked.Increment(ref _requestId);
        var keepAlive = callOptions.KeepAlive;
        var fin = callOptions.Fin || !keepAlive;
        var flag = (isSystemCall ? RpcFlags.SystemCall : RpcFlags.ServiceCall) |
                   (keepAlive ? RpcFlags.KeepAlive : 0) |
                   (fin ? RpcFlags.Fin : 0);

        return new Packet
        {
            Header = new PacketHeader
            {
                Length = 1,
                Flag = (uint)flag,
                RequestId = requestId,
                Service = serviceName,
                Handler = handlerName,
                Env = callOptions.EnvironmentOverride ?? _options.Environment
            },
            Body = Any.Pack(request)
        };
    }

    private async Task HeartbeatTickAsync()
    {
        var snapshot = _keepAliveConnections.ToArray();
        if (snapshot.Length == 0)
        {
            return;
        }

        foreach (var pair in snapshot)
        {
            try
            {
                if (!pair.Value.IsConnected)
                {
                    _keepAliveConnections.TryRemove(pair.Key, out _);
                    continue;
                }

                var heartbeatPacket = BuildPacket(
                    pair.Value.ServiceName,
                    "ping",
                    new PingRequest { Message = "HEARTBEAT" },
                    isSystemCall: true,
                    callOptions: new RpcCallOptions
                    {
                        KeepAlive = true,
                        Timeout = _options.HeartbeatTimeout
                    });

                await pair.Value.SendAsync(heartbeatPacket, _options.HeartbeatTimeout, CancellationToken.None).ConfigureAwait(false);
            }
            catch
            {
                if (_keepAliveConnections.TryRemove(pair.Key, out var stale))
                {
                    await stale.DisposeAsync().ConfigureAwait(false);
                }
            }
        }
    }

    private sealed class RpcConnection : IAsyncDisposable
    {
        private readonly string _serviceName;
        private readonly string _host;
        private readonly int _port;
        private readonly int _maxPacketSize;
        private readonly bool _keepAlive;
        private readonly string _environment;
        private readonly Func<long> _nextRequestId;
        private readonly TimeSpan _finTimeout;
        private readonly SemaphoreSlim _connectLock = new(1, 1);
        private readonly SemaphoreSlim _writeLock = new(1, 1);
        private readonly ConcurrentDictionary<long, TaskCompletionSource<Packet>> _pending = new();

        private TcpClient? _client;
        private NetworkStream? _stream;
        private CancellationTokenSource? _receiveLoopCts;
        private Task? _receiveLoopTask;
        private int _disposed;
        private long _lastAccessUnixMilliseconds = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds();

        public RpcConnection(
            string serviceName,
            string host,
            int port,
            int maxPacketSize,
            bool keepAlive,
            string environment,
            Func<long> nextRequestId,
            TimeSpan finTimeout)
        {
            _serviceName = serviceName;
            _host = host;
            _port = port;
            _maxPacketSize = maxPacketSize;
            _keepAlive = keepAlive;
            _environment = environment;
            _nextRequestId = nextRequestId;
            _finTimeout = finTimeout;
        }

        public string ServiceName => _serviceName;

        public bool IsConnected => _client?.Connected == true && _stream is not null;

        public bool IsIdleTimedOut(TimeSpan idleTimeout)
        {
            if (idleTimeout <= TimeSpan.Zero)
            {
                return false;
            }

            var lastAccessMs = Interlocked.Read(ref _lastAccessUnixMilliseconds);
            var idleMs = DateTimeOffset.UtcNow.ToUnixTimeMilliseconds() - lastAccessMs;
            return idleMs > idleTimeout.TotalMilliseconds;
        }

        public async Task EnsureConnectedAsync(CancellationToken cancellationToken)
        {
            if (IsConnected)
            {
                return;
            }

            await _connectLock.WaitAsync(cancellationToken).ConfigureAwait(false);
            try
            {
                if (IsConnected)
                {
                    return;
                }

                _client = new TcpClient();
                await _client.ConnectAsync(_host, _port, cancellationToken).ConfigureAwait(false);
                _stream = _client.GetStream();
                _receiveLoopCts = new CancellationTokenSource();
                _receiveLoopTask = Task.Run(() => ReceiveLoopAsync(_receiveLoopCts.Token));
            }
            finally
            {
                _connectLock.Release();
            }
        }

        public async Task<Packet> SendAsync(Packet packet, TimeSpan timeout, CancellationToken cancellationToken)
        {
            var stream = _stream ?? throw new RpcException("Connection is not ready");
            var requestId = packet.Header.RequestId;
            var waiter = new TaskCompletionSource<Packet>(TaskCreationOptions.RunContinuationsAsynchronously);

            if (!_pending.TryAdd(requestId, waiter))
            {
                throw new RpcException($"Duplicate request id: {requestId}");
            }

            try
            {
                var frame = PacketFrameCodec.Encode(packet);
                await _writeLock.WaitAsync(cancellationToken).ConfigureAwait(false);
                try
                {
                    await stream.WriteAsync(frame, cancellationToken).ConfigureAwait(false);
                    await stream.FlushAsync(cancellationToken).ConfigureAwait(false);
                    TouchAccessTime();
                }
                finally
                {
                    _writeLock.Release();
                }

                using var timeoutCts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
                timeoutCts.CancelAfter(timeout);
                using var registration = timeoutCts.Token.Register(() => waiter.TrySetCanceled(timeoutCts.Token));
                return await waiter.Task.ConfigureAwait(false);
            }
            catch (OperationCanceledException ex)
            {
                throw new TimeoutException($"Request timeout: {requestId}", ex);
            }
            catch (Exception ex)
            {
                throw new RpcException("Failed to send request", ex);
            }
            finally
            {
                _pending.TryRemove(requestId, out _);
            }
        }

        public async ValueTask DisposeAsync()
        {
            if (Interlocked.Exchange(ref _disposed, 1) != 0)
            {
                return;
            }

            await TrySendFinAsync().ConfigureAwait(false);

            _receiveLoopCts?.Cancel();

            if (_receiveLoopTask is not null)
            {
                try
                {
                    await _receiveLoopTask.ConfigureAwait(false);
                }
                catch
                {
                    // Ignore receive loop teardown errors during disposal.
                }
            }

            foreach (var pair in _pending)
            {
                pair.Value.TrySetException(new RpcException("Connection closed"));
            }

            _pending.Clear();

            if (_stream is not null)
            {
                await _stream.DisposeAsync().ConfigureAwait(false);
            }

            _client?.Dispose();
            _stream = null;
            _client = null;
        }

        private async Task TrySendFinAsync()
        {
            if (!IsConnected)
            {
                return;
            }

            var flag = RpcFlags.SystemCall |
                       RpcFlags.Fin |
                       (_keepAlive ? RpcFlags.KeepAlive : 0);

            var finPacket = new Packet
            {
                Header = new PacketHeader
                {
                    Length = 1,
                    Flag = (uint)flag,
                    RequestId = _nextRequestId(),
                    Service = _serviceName,
                    Handler = "ping",
                    Env = _environment
                },
                Body = Any.Pack(new PingRequest { Message = "FIN" })
            };

            try
            {
                await SendAsync(finPacket, _finTimeout, CancellationToken.None).ConfigureAwait(false);
            }
            catch
            {
                // FIN is best effort; disposal must continue even if FIN exchange fails.
            }
        }

        private async Task ReceiveLoopAsync(CancellationToken cancellationToken)
        {
            try
            {
                while (!cancellationToken.IsCancellationRequested)
                {
                    var stream = _stream;
                    if (stream is null)
                    {
                        break;
                    }

                    var packet = await PacketFrameCodec.ReadAsync(stream, _maxPacketSize, cancellationToken).ConfigureAwait(false);
                    if (packet is null)
                    {
                        break;
                    }

                    if (_pending.TryRemove(packet.Header.RequestId, out var waiter))
                    {
                        waiter.TrySetResult(packet);
                    }

                    TouchAccessTime();
                }
            }
            catch (Exception ex)
            {
                foreach (var pair in _pending)
                {
                    pair.Value.TrySetException(new RpcException("Connection receive loop failed", ex));
                }
            }
        }

        private void TouchAccessTime()
        {
            Interlocked.Exchange(ref _lastAccessUnixMilliseconds, DateTimeOffset.UtcNow.ToUnixTimeMilliseconds());
        }
    }

    private static class PacketFrameCodec
    {
        private const int LengthFieldOffset = 3;
        private const int LengthFieldSize = 4;
        private const int MinimumPacketSize = LengthFieldOffset + LengthFieldSize;

        public static byte[] Encode(Packet packet)
        {
            var raw = packet.ToByteArray();
            var length = raw.Length;

            if (raw.Length < MinimumPacketSize)
            {
                throw new RpcException($"Packet is too small: {raw.Length}");
            }

            for (var i = 0; i < LengthFieldSize; i++)
            {
                raw[LengthFieldOffset + i] = (byte)((length >> (i * 8)) & 0xFF);
            }

            return raw;
        }

        public static async Task<Packet?> ReadAsync(Stream stream, int maxPacketSize, CancellationToken cancellationToken)
        {
            var header = await ReadExactOrNullAsync(stream, MinimumPacketSize, cancellationToken).ConfigureAwait(false);
            if (header is null)
            {
                return null;
            }

            var packetLength = 0;
            for (var i = 0; i < LengthFieldSize; i++)
            {
                packetLength += header[LengthFieldOffset + i] << (i * 8);
            }

            if (packetLength < MinimumPacketSize || packetLength > maxPacketSize)
            {
                throw new RpcException($"Invalid packet length: {packetLength}");
            }

            var remaining = packetLength - MinimumPacketSize;
            var body = remaining == 0
                ? Array.Empty<byte>()
                : await ReadExactAsync(stream, remaining, cancellationToken).ConfigureAwait(false);

            var raw = new byte[packetLength];
            Buffer.BlockCopy(header, 0, raw, 0, header.Length);
            if (body.Length > 0)
            {
                Buffer.BlockCopy(body, 0, raw, header.Length, body.Length);
            }

            return Packet.Parser.ParseFrom(raw);
        }

        private static async Task<byte[]?> ReadExactOrNullAsync(Stream stream, int length, CancellationToken cancellationToken)
        {
            var buffer = new byte[length];
            var offset = 0;

            while (offset < length)
            {
                var read = await stream.ReadAsync(buffer.AsMemory(offset, length - offset), cancellationToken).ConfigureAwait(false);
                if (read == 0)
                {
                    if (offset == 0)
                    {
                        return null;
                    }

                    throw new RpcException("Unexpected end of stream while reading packet header");
                }

                offset += read;
            }

            return buffer;
        }

        private static async Task<byte[]> ReadExactAsync(Stream stream, int length, CancellationToken cancellationToken)
        {
            var buffer = new byte[length];
            var offset = 0;

            while (offset < length)
            {
                var read = await stream.ReadAsync(buffer.AsMemory(offset, length - offset), cancellationToken).ConfigureAwait(false);
                if (read == 0)
                {
                    throw new RpcException("Unexpected end of stream while reading packet body");
                }

                offset += read;
            }

            return buffer;
        }
    }
}

