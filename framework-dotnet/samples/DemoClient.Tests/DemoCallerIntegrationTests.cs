using DemoClient.Generated;
using DemoClient.Proto;
using YppRpc.Client;

namespace DemoClient.Tests;

public class DemoCallerIntegrationTests
{
    [Fact]
    [Trait("Category", "Integration")]
    public async Task EchoAsync_RealServer_ReturnsExpectedText()
    {
        // Keep local unit test runs fast unless e2e is explicitly enabled.
        if (!string.Equals(Environment.GetEnvironmentVariable("YPPRPC_RUN_E2E"), "1", StringComparison.Ordinal))
        {
            return;
        }

        var configPath = Path.Combine(AppContext.BaseDirectory, "appsettings.integration.json");
        var options = RpcClientOptionsLoader.LoadFromFile(configPath);

        await using var client = new RpcClient(options);
        await client.StartAsync();

        var caller = new DemoCaller(client);
        var expectedText = $"hello-e2e-{DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()}";

        var response = await caller.EchoAsync(
            new EchoRequest { Text = expectedText },
            new RpcCallOptions
            {
                KeepAlive = true,
                Timeout = TimeSpan.FromSeconds(3)
            });

        Assert.Equal(expectedText, response.Text);
    }

    [Fact]
    [Trait("Category", "Integration")]
    [Trait("Category", "Stress")]
    public async Task EchoAsync_RealServer_Stress_ConfigurableLoad()
    {
        if (!string.Equals(Environment.GetEnvironmentVariable("YPPRPC_RUN_E2E"), "1", StringComparison.Ordinal))
        {
            return;
        }

        var totalRequests = GetEnvIntOrDefault("YPPRPC_E2E_STRESS_TOTAL_REQUESTS", 100, 1, 20000);
        var concurrency = GetEnvIntOrDefault("YPPRPC_E2E_STRESS_CONCURRENCY", 10, 1, 256);

        var configPath = Path.Combine(AppContext.BaseDirectory, "appsettings.integration.json");
        var options = RpcClientOptionsLoader.LoadFromFile(configPath);

        await using var client = new RpcClient(options);
        await client.StartAsync();

        var caller = new DemoCaller(client);
        var success = 0;
        var errors = new List<Exception>();
        var lockObj = new object();
        var requestsPerWorker = totalRequests / concurrency;
        var remainder = totalRequests % concurrency;

        var tasks = Enumerable.Range(0, concurrency).Select(async workerIndex =>
        {
            var count = requestsPerWorker + (workerIndex < remainder ? 1 : 0);
            for (var i = 0; i < count; i++)
            {
                var text = $"stress-{workerIndex}-{i}-{DateTimeOffset.UtcNow.ToUnixTimeMilliseconds()}";
                try
                {
                    var response = await caller.EchoAsync(
                        new EchoRequest { Text = text },
                        new RpcCallOptions
                        {
                            KeepAlive = true,
                            Timeout = TimeSpan.FromSeconds(5)
                        });

                    if (!string.Equals(response.Text, text, StringComparison.Ordinal))
                    {
                        throw new InvalidOperationException($"Echo mismatch, expected '{text}', actual '{response.Text}'.");
                    }

                    Interlocked.Increment(ref success);
                }
                catch (Exception ex)
                {
                    lock (lockObj)
                    {
                        errors.Add(ex);
                    }
                }
            }
        });

        await Task.WhenAll(tasks);

        if (errors.Count > 0)
        {
            throw new AggregateException($"Stress test failed with {errors.Count} errors out of {totalRequests} requests.", errors);
        }

        Assert.Equal(totalRequests, success);
    }

    private static int GetEnvIntOrDefault(string key, int defaultValue, int minValue, int maxValue)
    {
        var raw = Environment.GetEnvironmentVariable(key);
        if (!int.TryParse(raw, out var value))
        {
            return defaultValue;
        }

        if (value < minValue)
        {
            return minValue;
        }

        if (value > maxValue)
        {
            return maxValue;
        }

        return value;
    }
}

