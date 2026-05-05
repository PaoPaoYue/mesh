using System.Threading;
using System.Threading.Tasks;
using Moq;
using Xunit;
using DemoClient.Generated;
using YppRpc.Client;

#nullable enable

namespace DemoClient.Tests;

public class DemoCallerTests
{
    [Fact]
    public async Task EchoAsync_CanBeMockedViaCallerInterface()
    {
        var request = new DemoClient.Proto.EchoRequest();
        var expected = new DemoClient.Proto.EchoResponse();
        var caller = new Mock<IDemoCaller>(MockBehavior.Strict);

        caller.Setup(x => x.EchoAsync(
                It.IsAny<DemoClient.Proto.EchoRequest>(),
                It.IsAny<RpcCallOptions?>(),
                It.IsAny<CancellationToken>()))
            .ReturnsAsync(expected);

        var response = await caller.Object.EchoAsync(request);

        Assert.Same(expected, response);
    }
}
