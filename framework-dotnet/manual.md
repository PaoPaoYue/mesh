# framework-dotnet 使用手册

## 1. 功能边界

当前版本聚焦最基础客户端能力：

- 与 Java server 建立 TCP 长连接
- 定时发送系统心跳 `ping`
- 发送业务请求并等待响应
- 基于 `.proto` 的 caller 脚手架生成

暂未覆盖：服务发现、负载均衡策略、服务端 SDK、完整观测体系。

## 2. 项目结构

- `src/YppRpc.Client/RpcClient.cs`
  - `RpcClient`：对外调用入口
  - 内部 `RpcConnection`：单连接收发与 pending 请求关联
  - `PacketFrameCodec`：按 Java 端约定注入/读取 `length` 字段
- `src/YppRpc.Client/RpcCallOptions.cs`
  - 超时、重试、连接标签、是否 keepAlive
- `tools/YppRpc.IdlScaffolder/Program.cs`
  - 从 proto 中解析 `rpc xxx(req) returns (resp)` 并生成 caller
- `samples/DemoClient`
  - 生成代码 + 示例调用

## 3. 如何接入你的服务

### 步骤 A：准备 proto

确保服务 proto 有 `service` 与 `rpc` 方法定义，例如：

```proto
service Demo {
  rpc echo(EchoRequest) returns (EchoResponse);
}
```

### 步骤 B：生成 caller

```powershell
$MeshRoot = "<你的mesh仓库根目录>"
Set-Location "$MeshRoot\framework-dotnet"
dotnet run --project .\tools\YppRpc.IdlScaffolder\YppRpc.IdlScaffolder.csproj -- --service-name demo-service --service-alias Demo --proto-dir <你的proto目录> --output-dir <生成目录> --namespace <你的生成代码命名空间> --proto-namespace <protoc生成的C#命名空间>
```

默认会同时生成 mock 示例测试与客户端配置模板；如需覆盖默认输出路径，可追加：

```powershell
dotnet run --project .\tools\YppRpc.IdlScaffolder\YppRpc.IdlScaffolder.csproj -- --service-name demo-service --service-alias Demo --proto-dir <你的proto目录> --output-dir <生成目录> --namespace <你的生成代码命名空间> --proto-namespace <protoc生成的C#命名空间> --test-output-dir <测试项目目录> --test-namespace <测试命名空间> --config-output-path <配置文件路径>
```

参数说明：

- `--service-name`：发包 header 中的 service 名（必须与 server 注册名一致）
- `--service-alias`：生成类名后缀，如 `Demo -> DemoCaller`
- `--proto-dir`：proto 文件目录（工具会扫描该目录下 `*.proto`）
- `--output-dir`：生成 `IxxCaller.cs` 和 `xxCaller.cs`
- `--namespace`：生成 caller 的 C# 命名空间
- `--proto-namespace`：你的 proto C# 类型命名空间
- `--test-output-dir`：可选，覆盖测试文件输出目录（默认自动推导）
- `--test-namespace`：可选，测试代码命名空间；未指定时默认从 `--namespace` 推导（如 `DemoClient.Generated -> DemoClient.Tests`）
- `--config-output-path`：可选，覆盖配置模板输出路径（默认自动推导）

### 步骤 C：创建并启动客户端

```csharp
var options = RpcClientOptionsLoader.LoadFromFile("appsettings.json");

await using var client = new RpcClient(options);
await client.StartAsync();
```

### 步骤 D：通过生成的 caller 发起调用

```csharp
var caller = new DemoCaller(client);
var response = await caller.EchoAsync(
    new DemoClient.Proto.EchoRequest { Text = "hello" },
    new RpcCallOptions
    {
        KeepAlive = true,
        Timeout = TimeSpan.FromSeconds(2),
        RetryTimes = 1,
        RetryInterval = TimeSpan.FromMilliseconds(200)
    });
```

### 步骤 E：已有 .NET 9.0 项目接入

如果你已经有一个 `net9.0` 项目，可以直接按下面方式引入。

1) 引入客户端库（推荐先用本地项目引用）

```powershell
$MeshRoot = "<你的mesh仓库根目录>"
dotnet add <你的项目.csproj> reference "$MeshRoot\framework-dotnet\src\YppRpc.Client\YppRpc.Client.csproj"
```

2) 在你的项目里生成 caller（输出到你自己的 `Generated` 目录）

```powershell
$MeshRoot = "<你的mesh仓库根目录>"
Set-Location "$MeshRoot\framework-dotnet"
dotnet run --project .\tools\YppRpc.IdlScaffolder\YppRpc.IdlScaffolder.csproj -- --service-name <服务名> --service-alias <别名> --proto-dir <你的proto目录> --output-dir <你的项目目录>\Generated --namespace <你的生成命名空间> --proto-namespace <你的proto类型命名空间>
```

若你希望覆盖配置模板默认输出路径：

```powershell
dotnet run --project .\tools\YppRpc.IdlScaffolder\YppRpc.IdlScaffolder.csproj -- --service-name <服务名> --service-alias <别名> --proto-dir <你的proto目录> --output-dir <你的项目目录>\Generated --namespace <你的生成命名空间> --proto-namespace <你的proto类型命名空间> --config-output-path <你的项目目录>\appsettings.json
```

3) 确保你的项目会生成 proto C# 类型（如果你项目里还没配置）

```xml
<ItemGroup>
  <PackageReference Include="Google.Protobuf" Version="3.27.2" />
  <PackageReference Include="Grpc.Tools" Version="2.65.0" PrivateAssets="all" />
</ItemGroup>

<ItemGroup>
  <Protobuf Include="proto\*.proto" ProtoRoot="proto" GrpcServices="None" />
</ItemGroup>
```

4) 在现有项目启动时初始化客户端并调用

```csharp
var options = RpcClientOptionsLoader.LoadFromFile("appsettings.json");

await using var client = new RpcClient(options);
await client.StartAsync();

var caller = new DemoCaller(client);
var response = await caller.EchoAsync(new DemoClient.Proto.EchoRequest { Text = "hello" });
```

注意：`--service-name` 必须与 Java server 注册名一致，`--proto-namespace` 必须与你项目实际生成的 protobuf C# 命名空间一致。

## 4. 长连接与心跳说明

- 默认 `KeepAlive = true`，会复用连接
- 连接池 key: `serviceName + connectionTag`
- 心跳包使用系统调用：
  - `service`: 目标服务名
  - `handler`: `ping`
  - body: `PingRequest { message = "HEARTBEAT" }`

## 4.1 Java 配置对齐（`mesh.rpc`）

`framework-dotnet` 现在默认兼容 Java client 的配置结构与字段命名：

```json
{
  "mesh": {
    "rpc": {
      "env": "local",
      "clientEnabled": true,
      "defaultClientService": {
        "name": "demo-service",
        "host": "127.0.0.1",
        "port": 8080
      },
      "clientServices": [
        {
          "name": "demo-service",
          "host": "127.0.0.1",
          "port": 8080
        }
      ],
      "clientShutDownTimeout": 10,
      "packetMaxSize": 1048576,
      "keepAliveTimeout": 6,
      "keepAliveInterval": 2,
      "keepAliveIdleTimeout": 10,
      "keepAliveHeartbeatTimeout": 1
    }
  }
}
```

字段对齐关系：

- `env` -> `RpcClientOptions.Environment`
- `packetMaxSize` -> `RpcClientOptions.MaxPacketSizeBytes`
- `keepAliveInterval` -> `RpcClientOptions.HeartbeatInterval`
- `keepAliveHeartbeatTimeout` -> `RpcClientOptions.HeartbeatTimeout`
- `clientShutDownTimeout` -> `RpcClientOptions.ClientShutdownTimeout`
- `keepAliveIdleTimeout` -> `RpcClientOptions.KeepAliveIdleTimeout`
- `clientServices[].name/host/port` -> `RpcClientOptions.Services`

兼容性说明：

- 旧格式 `YppRpc` 仍可继续使用。
- `RpcClientOptionsLoader.LoadFromFile("appsettings.json")` 会按顺序自动尝试：`YppRpc` -> `mesh.rpc`。

## 5. 常见问题

### Q1: 调用报 `Service endpoint is not configured`

未在 `RpcClientOptions.Services` 注册目标服务地址。

### Q2: 为什么 caller 生成后编译找不到请求/响应类型？

通常是 `--proto-namespace` 与你的 protobuf C# 命名空间不一致。

### Q3: 连接建立后偶发超时

优先检查：

- server 是否已启动且端口可达
- `Timeout` 是否太短
- 网络环境是否有代理/防火墙拦截

## 6. 本地验证命令

```powershell
$MeshRoot = "<你的mesh仓库根目录>"
Set-Location "$MeshRoot\framework-dotnet"
dotnet build YppRpc.DotNet.sln
```

如需演示生成器：

```powershell
$MeshRoot = "<你的mesh仓库根目录>"
Set-Location "$MeshRoot\framework-dotnet"
dotnet run --project .\tools\YppRpc.IdlScaffolder\YppRpc.IdlScaffolder.csproj -- --service-name demo-service --service-alias Demo --proto-dir .\samples\DemoClient\proto --output-dir .\samples\DemoClient\Generated --namespace DemoClient.Generated --proto-namespace DemoClient.Proto
```

覆盖测试模板与配置模板默认路径：

```powershell
dotnet run --project .\tools\YppRpc.IdlScaffolder\YppRpc.IdlScaffolder.csproj -- --service-name demo-service --service-alias Demo --proto-dir .\samples\DemoClient\proto --output-dir .\samples\DemoClient\Generated --namespace DemoClient.Generated --proto-namespace DemoClient.Proto --test-output-dir .\samples\DemoClient.Tests --config-output-path .\samples\DemoClient\appsettings.json
```

## 7. Demo 单测示例（RPC Method）

`samples/DemoClient.Tests/DemoCallerTests.cs` 提供了 `EchoAsync` 的单测示例，核心思路是：

- 使用 `Moq` mock `IDemoCaller`
- 让业务侧测试直接依赖 caller 接口，不再依赖 `IRpcInvoker`
- 校验请求参数和返回映射

运行命令：

```powershell
$MeshRoot = "<你的mesh仓库根目录>"
Set-Location "$MeshRoot\framework-dotnet"
dotnet test YppRpc.DotNet.sln
```

