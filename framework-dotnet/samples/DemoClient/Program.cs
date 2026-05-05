using DemoClient.Generated;
using YppRpc.Client;

var configPath = args.Length > 0 ? args[0] : "appsettings.json";
var options = RpcClientOptionsLoader.LoadFromFile(configPath);

await using var client = new RpcClient(options);
await client.StartAsync();

var caller = new DemoCaller(client);
var response = await caller.EchoAsync(
	new DemoClient.Proto.EchoRequest { Text = "hello from dotnet client" },
	new RpcCallOptions
	{
		KeepAlive = true,
		Timeout = TimeSpan.FromSeconds(2)
	});

Console.WriteLine($"response: {response.Text}");
