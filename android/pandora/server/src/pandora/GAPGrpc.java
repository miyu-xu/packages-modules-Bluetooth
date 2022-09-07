package pandora;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: pandora/gap.proto")
public final class GAPGrpc {

  private GAPGrpc() {}

  public static final String SERVICE_NAME = "pandora.GAP";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getMakeDiscoverableMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "MakeDiscoverable",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getMakeDiscoverableMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getMakeDiscoverableMethod;
    if ((getMakeDiscoverableMethod = GAPGrpc.getMakeDiscoverableMethod) == null) {
      synchronized (GAPGrpc.class) {
        if ((getMakeDiscoverableMethod = GAPGrpc.getMakeDiscoverableMethod) == null) {
          GAPGrpc.getMakeDiscoverableMethod = getMakeDiscoverableMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "MakeDiscoverable"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new GAPMethodDescriptorSupplier("MakeDiscoverable"))
              .build();
        }
      }
    }
    return getMakeDiscoverableMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static GAPStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GAPStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GAPStub>() {
        @java.lang.Override
        public GAPStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GAPStub(channel, callOptions);
        }
      };
    return GAPStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static GAPBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GAPBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GAPBlockingStub>() {
        @java.lang.Override
        public GAPBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GAPBlockingStub(channel, callOptions);
        }
      };
    return GAPBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static GAPFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<GAPFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<GAPFutureStub>() {
        @java.lang.Override
        public GAPFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new GAPFutureStub(channel, callOptions);
        }
      };
    return GAPFutureStub.newStub(factory, channel);
  }

  /**
   */
  public static abstract class GAPImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Make the IUT general Discoverable
     * Format: rpc &lt;func&gt;(&lt;request&gt;) returns (&lt;response&gt;)
     * </pre>
     */
    public void makeDiscoverable(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getMakeDiscoverableMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getMakeDiscoverableMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_MAKE_DISCOVERABLE)))
          .build();
    }
  }

  /**
   */
  public static final class GAPStub extends io.grpc.stub.AbstractAsyncStub<GAPStub> {
    private GAPStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GAPStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GAPStub(channel, callOptions);
    }

    /**
     * <pre>
     * Make the IUT general Discoverable
     * Format: rpc &lt;func&gt;(&lt;request&gt;) returns (&lt;response&gt;)
     * </pre>
     */
    public void makeDiscoverable(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getMakeDiscoverableMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class GAPBlockingStub extends io.grpc.stub.AbstractBlockingStub<GAPBlockingStub> {
    private GAPBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GAPBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GAPBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Make the IUT general Discoverable
     * Format: rpc &lt;func&gt;(&lt;request&gt;) returns (&lt;response&gt;)
     * </pre>
     */
    public com.google.protobuf.Empty makeDiscoverable(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getMakeDiscoverableMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class GAPFutureStub extends io.grpc.stub.AbstractFutureStub<GAPFutureStub> {
    private GAPFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected GAPFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new GAPFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Make the IUT general Discoverable
     * Format: rpc &lt;func&gt;(&lt;request&gt;) returns (&lt;response&gt;)
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> makeDiscoverable(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getMakeDiscoverableMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_MAKE_DISCOVERABLE = 0;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final GAPImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(GAPImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_MAKE_DISCOVERABLE:
          serviceImpl.makeDiscoverable((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(
        io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class GAPBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    GAPBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return pandora.GapProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("GAP");
    }
  }

  private static final class GAPFileDescriptorSupplier
      extends GAPBaseDescriptorSupplier {
    GAPFileDescriptorSupplier() {}
  }

  private static final class GAPMethodDescriptorSupplier
      extends GAPBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    GAPMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (GAPGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new GAPFileDescriptorSupplier())
              .addMethod(getMakeDiscoverableMethod())
              .build();
        }
      }
    }
    return result;
  }
}
