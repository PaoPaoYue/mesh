# YPP-RPC
[![Maven Central](https://img.shields.io/maven-central/v/io.github.paopaoyue/ypp-rpc.svg)](https://search.maven.org/artifact/io.github.paopaoyue/ypp-rpc) </br>
YPP-RPC is a lightweight RPC framework based on Springboot implemented with native Java socket interface.

## Features

- Support synchronous remote calls
- Springboot integration
- Microservices friendly
- Use Protobuf encoding
- Persistent connection and more connection control
- Timeout and retry mechanism
- Error handling
- Graceful Termination
- Easy mock Test
- Gradle plugin code generation

## Quick Start

> ✨ Your can find the example under `examples/demo` directory.

### Prerequisites

| application            | required version |
|------------------------|------------------|
| JDK                    | 21 or later      |
| Gradle(Gradle wrapper) | 8.6 or later     |
| protoc                 | 26.0 or later    |

> 💡Legacy **JDK 8** support is also available. While The **Gradle** and **protoc** version should be the same as the above.
> 
> In this case, please use **Springboot 2.X** along with the `jdk8` suffix version of the Gradle plugin `io.github.paopaoyue.ypp-rpc-generator`.

1. Create a new Springboot project
You can create a new Springboot project by using the [Spring Initializr](https://start.spring.io/). <br>
<b> ❗Springboot version should be 3.3.0 or later, java version should be 21 or later. </b>

2. Add the following to your `build.gradle` file:
```groovy
plugins {
    id 'io.github.paopaoyue.ypp-rpc-generator' version '0.0.18' // or 0.0.18-jdk8 for java 8 
}

rpcGenerator {
    serviceName = 'demo-service'
    serviceShortAlias = 'demo' // optional - default to serviceName
    protoRepoPath = 'idl' // optional - default to 'idl' directory from the project root
}
```

3. Run `gradlew generateIdl` to generate the idl files and modify the idl files to define your service.
4. Run `gradlew generateRpc` to generate the rpc files.
5. Configure the service endpoints in your `application.properties file:
```properties
mesh.rpc.server-enabled=true
mesh.rpc.server-service.name=demo-service
mesh.rpc.server-service.host=localhost
mesh.rpc.server-service.port=8080

mesh.rpc.client-enabled=true
mesh.rpc.client-services[0].name=demo-service
mesh.rpc.client-services[0].host=localhost
mesh.rpc.client-services[0].port=8080
```
6. Modify the generated `RpcService` class to implement your service logic.
```Java
    @RpcService(serviceName = "demo-service")
    public class DemoService implements IDemoService {
    
        @Override
        public DemoProto.EchoResponse echo(DemoProto.EchoRequest request) {
             return DemoProto.EchoResponse.newBuilder().setText(request.getText()).build();
        }
    }
```
7. Run your Springboot application with `gradlew bootRun` and you are ready to go!
```Java
    @Component
    public static class DemoRunner {

        @Autowired
        private IDemoCaller demoCaller;

        public void run() {
            var response = demoCaller.echo(DemoProto.EchoRequest.newBuilder().setText("hello world").build(), new CallOption());
            System.out.println(response.getText()); // should print "hello world"
        }
    }
```

## Mock Test
The framework has provided `@MockRpcService` and `@MockRpcCaller` annotations so that the user can use Mockito library to mock the service as well as the caller in their unit-test or integration test.
```Java
@SpringBootTest
class DemoApplicationTests {

    @Autowired
    IDemoCaller demoCaller;

    @Test
    void MockTest() {
        var response = demoCaller.echo(DemoProto.EchoRequest.newBuilder().setText("hello world").build(), new CallOption());
        assertThat(response.getText()).isEqualTo("noop!");
    }

    @TestConfiguration
    public static class TestConfig {

        @MockRpcService(serviceName = "demo-service")
        public IDemoService mockDemoService() {
            IDemoService service = mock(DemoService.class);
            when(service.echo(any())).thenAnswer(invocation -> {
                DemoProto.EchoRequest request = invocation.getArgument(0);
                return DemoProto.EchoResponse.newBuilder().setText("noop!").build();
            });
            return service;
        }
    }
}
```

## Roadmap

- [X] Basic RPC framework
- [X] Gradle plugin for code generation
- [ ] Sidecar for service discovery & load balancing in Kubernetes

## Benchmark
The benchmark is done by running the same service echo logic on ypp-rpc and grpc with 10 concurrent threads and each sends 10k requests.
![benchmark](benchmark.png)