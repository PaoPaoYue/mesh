# YPP-RPC .NET Client (MVP)

`framework-dotnet` 提供一个可与当前 Java Server 互通的 .NET 客户端最小实现，包含：

- 长连接复用（按 `serviceName + connectionTag`）
- 心跳保活（系统 `ping` 调用）
- 基于 IDL 的 C# Caller 脚手架生成工具
- Demo 客户端示例

> 当前是 **MVP**，只覆盖基础能力（连接、心跳、请求响应、基础重试），方便先落地跨语言客户端。

## 目录

- `src/YppRpc.Client`：核心客户端库
- `tools/YppRpc.IdlScaffolder`：IDL -> C# caller 生成工具
- `samples/DemoClient`：最小可运行示例
- `proto/rpc`：系统协议 proto（`protocol.proto` / `ping.proto` / `base.proto`）

## 已验证环境

- .NET SDK: `9.0.305`
- `dotnet build YppRpc.DotNet.sln`：通过

## 快速开始

### 1) 构建

```powershell
Set-Location "C:\Users\LENOVO\Desktop\projects\mesh\framework-dotnet"
dotnet build YppRpc.DotNet.sln
```

### 2) 根据 IDL 生成 caller 脚手架

```powershell
Set-Location "C:\Users\LENOVO\Desktop\projects\mesh\framework-dotnet"
dotnet run --project .\tools\YppRpc.IdlScaffolder\YppRpc.IdlScaffolder.csproj -- --service-name demo-service --service-alias Demo --proto-dir .\samples\DemoClient\proto --output-dir .\samples\DemoClient\Generated --namespace DemoClient.Generated --proto-namespace DemoClient.Proto
```

默认会自动生成 `DemoCallerTests.cs` 与 `appsettings.json`；如需覆盖路径，可额外传 `--test-output-dir`、`--config-output-path`。

### 3) 运行 Demo 客户端

```powershell
Set-Location "C:\Users\LENOVO\Desktop\projects\mesh\framework-dotnet"
dotnet run --project .\samples\DemoClient\DemoClient.csproj
```

默认会读取 `samples/DemoClient/appsettings.json` 的 `YppRpc` 配置。如果 server 已启动并提供 `demo-service/echo`，将打印 echo 响应。

## 详细使用说明

见 `framework-dotnet/manual.md`。

## Demo 单测示例

- 单测项目：`samples/DemoClient.Tests`
- 示例用例：`samples/DemoClient.Tests/DemoCallerTests.cs`

运行方式：

```powershell
Set-Location "<你的mesh仓库根目录>\framework-dotnet"
dotnet test YppRpc.DotNet.sln
```

