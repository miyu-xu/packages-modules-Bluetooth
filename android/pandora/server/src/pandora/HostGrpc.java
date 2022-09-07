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
 * <pre>
 * Service to trigger Bluetooth Host procedures
 * At startup, the Host must be in BR/EDR connectable mode
 * (see GAP connectability modes)
 * </pre>
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler",
    comments = "Source: pandora/host.proto")
public final class HostGrpc {

  private HostGrpc() {}

  public static final String SERVICE_NAME = "pandora.Host";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getResetMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Reset",
      requestType = com.google.protobuf.Empty.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      com.google.protobuf.Empty> getResetMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, com.google.protobuf.Empty> getResetMethod;
    if ((getResetMethod = HostGrpc.getResetMethod) == null) {
      synchronized (HostGrpc.class) {
        if ((getResetMethod = HostGrpc.getResetMethod) == null) {
          HostGrpc.getResetMethod = getResetMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Reset"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new HostMethodDescriptorSupplier("Reset"))
              .build();
        }
      }
    }
    return getResetMethod;
  }

  private static volatile io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      pandora.HostProto.ReadLocalAddressResponse> getReadLocalAddressMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ReadLocalAddress",
      requestType = com.google.protobuf.Empty.class,
      responseType = pandora.HostProto.ReadLocalAddressResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<com.google.protobuf.Empty,
      pandora.HostProto.ReadLocalAddressResponse> getReadLocalAddressMethod() {
    io.grpc.MethodDescriptor<com.google.protobuf.Empty, pandora.HostProto.ReadLocalAddressResponse> getReadLocalAddressMethod;
    if ((getReadLocalAddressMethod = HostGrpc.getReadLocalAddressMethod) == null) {
      synchronized (HostGrpc.class) {
        if ((getReadLocalAddressMethod = HostGrpc.getReadLocalAddressMethod) == null) {
          HostGrpc.getReadLocalAddressMethod = getReadLocalAddressMethod =
              io.grpc.MethodDescriptor.<com.google.protobuf.Empty, pandora.HostProto.ReadLocalAddressResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ReadLocalAddress"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.ReadLocalAddressResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HostMethodDescriptorSupplier("ReadLocalAddress"))
              .build();
        }
      }
    }
    return getReadLocalAddressMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.HostProto.ConnectRequest,
      pandora.HostProto.ConnectResponse> getConnectMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Connect",
      requestType = pandora.HostProto.ConnectRequest.class,
      responseType = pandora.HostProto.ConnectResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.HostProto.ConnectRequest,
      pandora.HostProto.ConnectResponse> getConnectMethod() {
    io.grpc.MethodDescriptor<pandora.HostProto.ConnectRequest, pandora.HostProto.ConnectResponse> getConnectMethod;
    if ((getConnectMethod = HostGrpc.getConnectMethod) == null) {
      synchronized (HostGrpc.class) {
        if ((getConnectMethod = HostGrpc.getConnectMethod) == null) {
          HostGrpc.getConnectMethod = getConnectMethod =
              io.grpc.MethodDescriptor.<pandora.HostProto.ConnectRequest, pandora.HostProto.ConnectResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Connect"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.ConnectRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.ConnectResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HostMethodDescriptorSupplier("Connect"))
              .build();
        }
      }
    }
    return getConnectMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.HostProto.GetConnectionRequest,
      pandora.HostProto.GetConnectionResponse> getGetConnectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetConnection",
      requestType = pandora.HostProto.GetConnectionRequest.class,
      responseType = pandora.HostProto.GetConnectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.HostProto.GetConnectionRequest,
      pandora.HostProto.GetConnectionResponse> getGetConnectionMethod() {
    io.grpc.MethodDescriptor<pandora.HostProto.GetConnectionRequest, pandora.HostProto.GetConnectionResponse> getGetConnectionMethod;
    if ((getGetConnectionMethod = HostGrpc.getGetConnectionMethod) == null) {
      synchronized (HostGrpc.class) {
        if ((getGetConnectionMethod = HostGrpc.getGetConnectionMethod) == null) {
          HostGrpc.getGetConnectionMethod = getGetConnectionMethod =
              io.grpc.MethodDescriptor.<pandora.HostProto.GetConnectionRequest, pandora.HostProto.GetConnectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetConnection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.GetConnectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.GetConnectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HostMethodDescriptorSupplier("GetConnection"))
              .build();
        }
      }
    }
    return getGetConnectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.HostProto.WaitConnectionRequest,
      pandora.HostProto.WaitConnectionResponse> getWaitConnectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "WaitConnection",
      requestType = pandora.HostProto.WaitConnectionRequest.class,
      responseType = pandora.HostProto.WaitConnectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.HostProto.WaitConnectionRequest,
      pandora.HostProto.WaitConnectionResponse> getWaitConnectionMethod() {
    io.grpc.MethodDescriptor<pandora.HostProto.WaitConnectionRequest, pandora.HostProto.WaitConnectionResponse> getWaitConnectionMethod;
    if ((getWaitConnectionMethod = HostGrpc.getWaitConnectionMethod) == null) {
      synchronized (HostGrpc.class) {
        if ((getWaitConnectionMethod = HostGrpc.getWaitConnectionMethod) == null) {
          HostGrpc.getWaitConnectionMethod = getWaitConnectionMethod =
              io.grpc.MethodDescriptor.<pandora.HostProto.WaitConnectionRequest, pandora.HostProto.WaitConnectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "WaitConnection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.WaitConnectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.WaitConnectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HostMethodDescriptorSupplier("WaitConnection"))
              .build();
        }
      }
    }
    return getWaitConnectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.HostProto.DisconnectRequest,
      pandora.HostProto.DisconnectResponse> getDisconnectMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "Disconnect",
      requestType = pandora.HostProto.DisconnectRequest.class,
      responseType = pandora.HostProto.DisconnectResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.HostProto.DisconnectRequest,
      pandora.HostProto.DisconnectResponse> getDisconnectMethod() {
    io.grpc.MethodDescriptor<pandora.HostProto.DisconnectRequest, pandora.HostProto.DisconnectResponse> getDisconnectMethod;
    if ((getDisconnectMethod = HostGrpc.getDisconnectMethod) == null) {
      synchronized (HostGrpc.class) {
        if ((getDisconnectMethod = HostGrpc.getDisconnectMethod) == null) {
          HostGrpc.getDisconnectMethod = getDisconnectMethod =
              io.grpc.MethodDescriptor.<pandora.HostProto.DisconnectRequest, pandora.HostProto.DisconnectResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Disconnect"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.DisconnectRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.DisconnectResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HostMethodDescriptorSupplier("Disconnect"))
              .build();
        }
      }
    }
    return getDisconnectMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.HostProto.ConnectLERequest,
      pandora.HostProto.ConnectLEResponse> getConnectLEMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "ConnectLE",
      requestType = pandora.HostProto.ConnectLERequest.class,
      responseType = pandora.HostProto.ConnectLEResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.HostProto.ConnectLERequest,
      pandora.HostProto.ConnectLEResponse> getConnectLEMethod() {
    io.grpc.MethodDescriptor<pandora.HostProto.ConnectLERequest, pandora.HostProto.ConnectLEResponse> getConnectLEMethod;
    if ((getConnectLEMethod = HostGrpc.getConnectLEMethod) == null) {
      synchronized (HostGrpc.class) {
        if ((getConnectLEMethod = HostGrpc.getConnectLEMethod) == null) {
          HostGrpc.getConnectLEMethod = getConnectLEMethod =
              io.grpc.MethodDescriptor.<pandora.HostProto.ConnectLERequest, pandora.HostProto.ConnectLEResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ConnectLE"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.ConnectLERequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.ConnectLEResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HostMethodDescriptorSupplier("ConnectLE"))
              .build();
        }
      }
    }
    return getConnectLEMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.HostProto.GetLEConnectionRequest,
      pandora.HostProto.GetLEConnectionResponse> getGetLEConnectionMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "GetLEConnection",
      requestType = pandora.HostProto.GetLEConnectionRequest.class,
      responseType = pandora.HostProto.GetLEConnectionResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.HostProto.GetLEConnectionRequest,
      pandora.HostProto.GetLEConnectionResponse> getGetLEConnectionMethod() {
    io.grpc.MethodDescriptor<pandora.HostProto.GetLEConnectionRequest, pandora.HostProto.GetLEConnectionResponse> getGetLEConnectionMethod;
    if ((getGetLEConnectionMethod = HostGrpc.getGetLEConnectionMethod) == null) {
      synchronized (HostGrpc.class) {
        if ((getGetLEConnectionMethod = HostGrpc.getGetLEConnectionMethod) == null) {
          HostGrpc.getGetLEConnectionMethod = getGetLEConnectionMethod =
              io.grpc.MethodDescriptor.<pandora.HostProto.GetLEConnectionRequest, pandora.HostProto.GetLEConnectionResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "GetLEConnection"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.GetLEConnectionRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.GetLEConnectionResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HostMethodDescriptorSupplier("GetLEConnection"))
              .build();
        }
      }
    }
    return getGetLEConnectionMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.HostProto.DisconnectLERequest,
      com.google.protobuf.Empty> getDisconnectLEMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DisconnectLE",
      requestType = pandora.HostProto.DisconnectLERequest.class,
      responseType = com.google.protobuf.Empty.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.HostProto.DisconnectLERequest,
      com.google.protobuf.Empty> getDisconnectLEMethod() {
    io.grpc.MethodDescriptor<pandora.HostProto.DisconnectLERequest, com.google.protobuf.Empty> getDisconnectLEMethod;
    if ((getDisconnectLEMethod = HostGrpc.getDisconnectLEMethod) == null) {
      synchronized (HostGrpc.class) {
        if ((getDisconnectLEMethod = HostGrpc.getDisconnectLEMethod) == null) {
          HostGrpc.getDisconnectLEMethod = getDisconnectLEMethod =
              io.grpc.MethodDescriptor.<pandora.HostProto.DisconnectLERequest, com.google.protobuf.Empty>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DisconnectLE"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.DisconnectLERequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  com.google.protobuf.Empty.getDefaultInstance()))
              .setSchemaDescriptor(new HostMethodDescriptorSupplier("DisconnectLE"))
              .build();
        }
      }
    }
    return getDisconnectLEMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.HostProto.PairingEventAnswer,
      pandora.HostProto.PairingEvent> getOnPairingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "OnPairing",
      requestType = pandora.HostProto.PairingEventAnswer.class,
      responseType = pandora.HostProto.PairingEvent.class,
      methodType = io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
  public static io.grpc.MethodDescriptor<pandora.HostProto.PairingEventAnswer,
      pandora.HostProto.PairingEvent> getOnPairingMethod() {
    io.grpc.MethodDescriptor<pandora.HostProto.PairingEventAnswer, pandora.HostProto.PairingEvent> getOnPairingMethod;
    if ((getOnPairingMethod = HostGrpc.getOnPairingMethod) == null) {
      synchronized (HostGrpc.class) {
        if ((getOnPairingMethod = HostGrpc.getOnPairingMethod) == null) {
          HostGrpc.getOnPairingMethod = getOnPairingMethod =
              io.grpc.MethodDescriptor.<pandora.HostProto.PairingEventAnswer, pandora.HostProto.PairingEvent>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "OnPairing"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.PairingEventAnswer.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.PairingEvent.getDefaultInstance()))
              .setSchemaDescriptor(new HostMethodDescriptorSupplier("OnPairing"))
              .build();
        }
      }
    }
    return getOnPairingMethod;
  }

  private static volatile io.grpc.MethodDescriptor<pandora.HostProto.DeletePairingRequest,
      pandora.HostProto.DeletePairingResponse> getDeletePairingMethod;

  @io.grpc.stub.annotations.RpcMethod(
      fullMethodName = SERVICE_NAME + '/' + "DeletePairing",
      requestType = pandora.HostProto.DeletePairingRequest.class,
      responseType = pandora.HostProto.DeletePairingResponse.class,
      methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
  public static io.grpc.MethodDescriptor<pandora.HostProto.DeletePairingRequest,
      pandora.HostProto.DeletePairingResponse> getDeletePairingMethod() {
    io.grpc.MethodDescriptor<pandora.HostProto.DeletePairingRequest, pandora.HostProto.DeletePairingResponse> getDeletePairingMethod;
    if ((getDeletePairingMethod = HostGrpc.getDeletePairingMethod) == null) {
      synchronized (HostGrpc.class) {
        if ((getDeletePairingMethod = HostGrpc.getDeletePairingMethod) == null) {
          HostGrpc.getDeletePairingMethod = getDeletePairingMethod =
              io.grpc.MethodDescriptor.<pandora.HostProto.DeletePairingRequest, pandora.HostProto.DeletePairingResponse>newBuilder()
              .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
              .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeletePairing"))
              .setSampledToLocalTracing(true)
              .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.DeletePairingRequest.getDefaultInstance()))
              .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                  pandora.HostProto.DeletePairingResponse.getDefaultInstance()))
              .setSchemaDescriptor(new HostMethodDescriptorSupplier("DeletePairing"))
              .build();
        }
      }
    }
    return getDeletePairingMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static HostStub newStub(io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HostStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HostStub>() {
        @java.lang.Override
        public HostStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HostStub(channel, callOptions);
        }
      };
    return HostStub.newStub(factory, channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static HostBlockingStub newBlockingStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HostBlockingStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HostBlockingStub>() {
        @java.lang.Override
        public HostBlockingStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HostBlockingStub(channel, callOptions);
        }
      };
    return HostBlockingStub.newStub(factory, channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static HostFutureStub newFutureStub(
      io.grpc.Channel channel) {
    io.grpc.stub.AbstractStub.StubFactory<HostFutureStub> factory =
      new io.grpc.stub.AbstractStub.StubFactory<HostFutureStub>() {
        @java.lang.Override
        public HostFutureStub newStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
          return new HostFutureStub(channel, callOptions);
        }
      };
    return HostFutureStub.newStub(factory, channel);
  }

  /**
   * <pre>
   * Service to trigger Bluetooth Host procedures
   * At startup, the Host must be in BR/EDR connectable mode
   * (see GAP connectability modes)
   * </pre>
   */
  public static abstract class HostImplBase implements io.grpc.BindableService {

    /**
     * <pre>
     * Reset the host.
     * **After** responding to this command, the GRPC server should loose
     * all its state.
     * This is comparable to a process restart or an hardware reset.
     * The GRPC server might take some time to be available after
     * this command.
     * </pre>
     */
    public void reset(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getResetMethod(), responseObserver);
    }

    /**
     * <pre>
     * Read the local Bluetooth device address.
     * This should return the same value as a Read BD_ADDR HCI command.
     * </pre>
     */
    public void readLocalAddress(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<pandora.HostProto.ReadLocalAddressResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getReadLocalAddressMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create an ACL BR/EDR connection to a peer.
     * This should send a CreateConnection on the HCI level.
     * If the two devices have not established a previous bond,
     * the peer must be discoverable.
     * </pre>
     */
    public void connect(pandora.HostProto.ConnectRequest request,
        io.grpc.stub.StreamObserver<pandora.HostProto.ConnectResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getConnectMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get an active ACL BR/EDR connection to a peer.
     * </pre>
     */
    public void getConnection(pandora.HostProto.GetConnectionRequest request,
        io.grpc.stub.StreamObserver<pandora.HostProto.GetConnectionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetConnectionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Wait for an ACL BR/EDR connection from a peer.
     * </pre>
     */
    public void waitConnection(pandora.HostProto.WaitConnectionRequest request,
        io.grpc.stub.StreamObserver<pandora.HostProto.WaitConnectionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getWaitConnectionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Disconnect an ACL BR/EDR connection. The Connection must not be reused
     * afterwards.
     * </pre>
     */
    public void disconnect(pandora.HostProto.DisconnectRequest request,
        io.grpc.stub.StreamObserver<pandora.HostProto.DisconnectResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDisconnectMethod(), responseObserver);
    }

    /**
     * <pre>
     * Create a LE connection.
     * </pre>
     */
    public void connectLE(pandora.HostProto.ConnectLERequest request,
        io.grpc.stub.StreamObserver<pandora.HostProto.ConnectLEResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getConnectLEMethod(), responseObserver);
    }

    /**
     * <pre>
     * Get an active LE connection to a peer.
     * </pre>
     */
    public void getLEConnection(pandora.HostProto.GetLEConnectionRequest request,
        io.grpc.stub.StreamObserver<pandora.HostProto.GetLEConnectionResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getGetLEConnectionMethod(), responseObserver);
    }

    /**
     * <pre>
     * Disconnect ongoing LE connection.
     * </pre>
     */
    public void disconnectLE(pandora.HostProto.DisconnectLERequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnimplementedUnaryCall(getDisconnectLEMethod(), responseObserver);
    }

    /**
     * <pre>
     * Listen to pairing events.
     * This is handled independently from connections for several reasons:
     * - Pairing can be triggered at any time and multiple times during the
     *   lifetime of a connection (this also explains why this is a stream).
     * - In BR/EDR, the specification allows for a device to authenticate before
     *   connecting when in security mode 3 (link level enforced security).
     * </pre>
     */
    public io.grpc.stub.StreamObserver<pandora.HostProto.PairingEventAnswer> onPairing(
        io.grpc.stub.StreamObserver<pandora.HostProto.PairingEvent> responseObserver) {
      return asyncUnimplementedStreamingCall(getOnPairingMethod(), responseObserver);
    }

    /**
     * <pre>
     * Remove pairing.
     * </pre>
     */
    public void deletePairing(pandora.HostProto.DeletePairingRequest request,
        io.grpc.stub.StreamObserver<pandora.HostProto.DeletePairingResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeletePairingMethod(), responseObserver);
    }

    @java.lang.Override public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(
            getResetMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                com.google.protobuf.Empty>(
                  this, METHODID_RESET)))
          .addMethod(
            getReadLocalAddressMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                com.google.protobuf.Empty,
                pandora.HostProto.ReadLocalAddressResponse>(
                  this, METHODID_READ_LOCAL_ADDRESS)))
          .addMethod(
            getConnectMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.HostProto.ConnectRequest,
                pandora.HostProto.ConnectResponse>(
                  this, METHODID_CONNECT)))
          .addMethod(
            getGetConnectionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.HostProto.GetConnectionRequest,
                pandora.HostProto.GetConnectionResponse>(
                  this, METHODID_GET_CONNECTION)))
          .addMethod(
            getWaitConnectionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.HostProto.WaitConnectionRequest,
                pandora.HostProto.WaitConnectionResponse>(
                  this, METHODID_WAIT_CONNECTION)))
          .addMethod(
            getDisconnectMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.HostProto.DisconnectRequest,
                pandora.HostProto.DisconnectResponse>(
                  this, METHODID_DISCONNECT)))
          .addMethod(
            getConnectLEMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.HostProto.ConnectLERequest,
                pandora.HostProto.ConnectLEResponse>(
                  this, METHODID_CONNECT_LE)))
          .addMethod(
            getGetLEConnectionMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.HostProto.GetLEConnectionRequest,
                pandora.HostProto.GetLEConnectionResponse>(
                  this, METHODID_GET_LECONNECTION)))
          .addMethod(
            getDisconnectLEMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.HostProto.DisconnectLERequest,
                com.google.protobuf.Empty>(
                  this, METHODID_DISCONNECT_LE)))
          .addMethod(
            getOnPairingMethod(),
            asyncBidiStreamingCall(
              new MethodHandlers<
                pandora.HostProto.PairingEventAnswer,
                pandora.HostProto.PairingEvent>(
                  this, METHODID_ON_PAIRING)))
          .addMethod(
            getDeletePairingMethod(),
            asyncUnaryCall(
              new MethodHandlers<
                pandora.HostProto.DeletePairingRequest,
                pandora.HostProto.DeletePairingResponse>(
                  this, METHODID_DELETE_PAIRING)))
          .build();
    }
  }

  /**
   * <pre>
   * Service to trigger Bluetooth Host procedures
   * At startup, the Host must be in BR/EDR connectable mode
   * (see GAP connectability modes)
   * </pre>
   */
  public static final class HostStub extends io.grpc.stub.AbstractAsyncStub<HostStub> {
    private HostStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HostStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HostStub(channel, callOptions);
    }

    /**
     * <pre>
     * Reset the host.
     * **After** responding to this command, the GRPC server should loose
     * all its state.
     * This is comparable to a process restart or an hardware reset.
     * The GRPC server might take some time to be available after
     * this command.
     * </pre>
     */
    public void reset(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getResetMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Read the local Bluetooth device address.
     * This should return the same value as a Read BD_ADDR HCI command.
     * </pre>
     */
    public void readLocalAddress(com.google.protobuf.Empty request,
        io.grpc.stub.StreamObserver<pandora.HostProto.ReadLocalAddressResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getReadLocalAddressMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create an ACL BR/EDR connection to a peer.
     * This should send a CreateConnection on the HCI level.
     * If the two devices have not established a previous bond,
     * the peer must be discoverable.
     * </pre>
     */
    public void connect(pandora.HostProto.ConnectRequest request,
        io.grpc.stub.StreamObserver<pandora.HostProto.ConnectResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getConnectMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get an active ACL BR/EDR connection to a peer.
     * </pre>
     */
    public void getConnection(pandora.HostProto.GetConnectionRequest request,
        io.grpc.stub.StreamObserver<pandora.HostProto.GetConnectionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetConnectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Wait for an ACL BR/EDR connection from a peer.
     * </pre>
     */
    public void waitConnection(pandora.HostProto.WaitConnectionRequest request,
        io.grpc.stub.StreamObserver<pandora.HostProto.WaitConnectionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getWaitConnectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Disconnect an ACL BR/EDR connection. The Connection must not be reused
     * afterwards.
     * </pre>
     */
    public void disconnect(pandora.HostProto.DisconnectRequest request,
        io.grpc.stub.StreamObserver<pandora.HostProto.DisconnectResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDisconnectMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Create a LE connection.
     * </pre>
     */
    public void connectLE(pandora.HostProto.ConnectLERequest request,
        io.grpc.stub.StreamObserver<pandora.HostProto.ConnectLEResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getConnectLEMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Get an active LE connection to a peer.
     * </pre>
     */
    public void getLEConnection(pandora.HostProto.GetLEConnectionRequest request,
        io.grpc.stub.StreamObserver<pandora.HostProto.GetLEConnectionResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getGetLEConnectionMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Disconnect ongoing LE connection.
     * </pre>
     */
    public void disconnectLE(pandora.HostProto.DisconnectLERequest request,
        io.grpc.stub.StreamObserver<com.google.protobuf.Empty> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDisconnectLEMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     * <pre>
     * Listen to pairing events.
     * This is handled independently from connections for several reasons:
     * - Pairing can be triggered at any time and multiple times during the
     *   lifetime of a connection (this also explains why this is a stream).
     * - In BR/EDR, the specification allows for a device to authenticate before
     *   connecting when in security mode 3 (link level enforced security).
     * </pre>
     */
    public io.grpc.stub.StreamObserver<pandora.HostProto.PairingEventAnswer> onPairing(
        io.grpc.stub.StreamObserver<pandora.HostProto.PairingEvent> responseObserver) {
      return asyncBidiStreamingCall(
          getChannel().newCall(getOnPairingMethod(), getCallOptions()), responseObserver);
    }

    /**
     * <pre>
     * Remove pairing.
     * </pre>
     */
    public void deletePairing(pandora.HostProto.DeletePairingRequest request,
        io.grpc.stub.StreamObserver<pandora.HostProto.DeletePairingResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getDeletePairingMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   * <pre>
   * Service to trigger Bluetooth Host procedures
   * At startup, the Host must be in BR/EDR connectable mode
   * (see GAP connectability modes)
   * </pre>
   */
  public static final class HostBlockingStub extends io.grpc.stub.AbstractBlockingStub<HostBlockingStub> {
    private HostBlockingStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HostBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HostBlockingStub(channel, callOptions);
    }

    /**
     * <pre>
     * Reset the host.
     * **After** responding to this command, the GRPC server should loose
     * all its state.
     * This is comparable to a process restart or an hardware reset.
     * The GRPC server might take some time to be available after
     * this command.
     * </pre>
     */
    public com.google.protobuf.Empty reset(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getResetMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Read the local Bluetooth device address.
     * This should return the same value as a Read BD_ADDR HCI command.
     * </pre>
     */
    public pandora.HostProto.ReadLocalAddressResponse readLocalAddress(com.google.protobuf.Empty request) {
      return blockingUnaryCall(
          getChannel(), getReadLocalAddressMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create an ACL BR/EDR connection to a peer.
     * This should send a CreateConnection on the HCI level.
     * If the two devices have not established a previous bond,
     * the peer must be discoverable.
     * </pre>
     */
    public pandora.HostProto.ConnectResponse connect(pandora.HostProto.ConnectRequest request) {
      return blockingUnaryCall(
          getChannel(), getConnectMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get an active ACL BR/EDR connection to a peer.
     * </pre>
     */
    public pandora.HostProto.GetConnectionResponse getConnection(pandora.HostProto.GetConnectionRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetConnectionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Wait for an ACL BR/EDR connection from a peer.
     * </pre>
     */
    public pandora.HostProto.WaitConnectionResponse waitConnection(pandora.HostProto.WaitConnectionRequest request) {
      return blockingUnaryCall(
          getChannel(), getWaitConnectionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Disconnect an ACL BR/EDR connection. The Connection must not be reused
     * afterwards.
     * </pre>
     */
    public pandora.HostProto.DisconnectResponse disconnect(pandora.HostProto.DisconnectRequest request) {
      return blockingUnaryCall(
          getChannel(), getDisconnectMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Create a LE connection.
     * </pre>
     */
    public pandora.HostProto.ConnectLEResponse connectLE(pandora.HostProto.ConnectLERequest request) {
      return blockingUnaryCall(
          getChannel(), getConnectLEMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Get an active LE connection to a peer.
     * </pre>
     */
    public pandora.HostProto.GetLEConnectionResponse getLEConnection(pandora.HostProto.GetLEConnectionRequest request) {
      return blockingUnaryCall(
          getChannel(), getGetLEConnectionMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Disconnect ongoing LE connection.
     * </pre>
     */
    public com.google.protobuf.Empty disconnectLE(pandora.HostProto.DisconnectLERequest request) {
      return blockingUnaryCall(
          getChannel(), getDisconnectLEMethod(), getCallOptions(), request);
    }

    /**
     * <pre>
     * Remove pairing.
     * </pre>
     */
    public pandora.HostProto.DeletePairingResponse deletePairing(pandora.HostProto.DeletePairingRequest request) {
      return blockingUnaryCall(
          getChannel(), getDeletePairingMethod(), getCallOptions(), request);
    }
  }

  /**
   * <pre>
   * Service to trigger Bluetooth Host procedures
   * At startup, the Host must be in BR/EDR connectable mode
   * (see GAP connectability modes)
   * </pre>
   */
  public static final class HostFutureStub extends io.grpc.stub.AbstractFutureStub<HostFutureStub> {
    private HostFutureStub(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected HostFutureStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new HostFutureStub(channel, callOptions);
    }

    /**
     * <pre>
     * Reset the host.
     * **After** responding to this command, the GRPC server should loose
     * all its state.
     * This is comparable to a process restart or an hardware reset.
     * The GRPC server might take some time to be available after
     * this command.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> reset(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getResetMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Read the local Bluetooth device address.
     * This should return the same value as a Read BD_ADDR HCI command.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.HostProto.ReadLocalAddressResponse> readLocalAddress(
        com.google.protobuf.Empty request) {
      return futureUnaryCall(
          getChannel().newCall(getReadLocalAddressMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create an ACL BR/EDR connection to a peer.
     * This should send a CreateConnection on the HCI level.
     * If the two devices have not established a previous bond,
     * the peer must be discoverable.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.HostProto.ConnectResponse> connect(
        pandora.HostProto.ConnectRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getConnectMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get an active ACL BR/EDR connection to a peer.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.HostProto.GetConnectionResponse> getConnection(
        pandora.HostProto.GetConnectionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetConnectionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Wait for an ACL BR/EDR connection from a peer.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.HostProto.WaitConnectionResponse> waitConnection(
        pandora.HostProto.WaitConnectionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getWaitConnectionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Disconnect an ACL BR/EDR connection. The Connection must not be reused
     * afterwards.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.HostProto.DisconnectResponse> disconnect(
        pandora.HostProto.DisconnectRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDisconnectMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Create a LE connection.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.HostProto.ConnectLEResponse> connectLE(
        pandora.HostProto.ConnectLERequest request) {
      return futureUnaryCall(
          getChannel().newCall(getConnectLEMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Get an active LE connection to a peer.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.HostProto.GetLEConnectionResponse> getLEConnection(
        pandora.HostProto.GetLEConnectionRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getGetLEConnectionMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Disconnect ongoing LE connection.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<com.google.protobuf.Empty> disconnectLE(
        pandora.HostProto.DisconnectLERequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDisconnectLEMethod(), getCallOptions()), request);
    }

    /**
     * <pre>
     * Remove pairing.
     * </pre>
     */
    public com.google.common.util.concurrent.ListenableFuture<pandora.HostProto.DeletePairingResponse> deletePairing(
        pandora.HostProto.DeletePairingRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getDeletePairingMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_RESET = 0;
  private static final int METHODID_READ_LOCAL_ADDRESS = 1;
  private static final int METHODID_CONNECT = 2;
  private static final int METHODID_GET_CONNECTION = 3;
  private static final int METHODID_WAIT_CONNECTION = 4;
  private static final int METHODID_DISCONNECT = 5;
  private static final int METHODID_CONNECT_LE = 6;
  private static final int METHODID_GET_LECONNECTION = 7;
  private static final int METHODID_DISCONNECT_LE = 8;
  private static final int METHODID_DELETE_PAIRING = 9;
  private static final int METHODID_ON_PAIRING = 10;

  private static final class MethodHandlers<Req, Resp> implements
      io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
      io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final HostImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(HostImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_RESET:
          serviceImpl.reset((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_READ_LOCAL_ADDRESS:
          serviceImpl.readLocalAddress((com.google.protobuf.Empty) request,
              (io.grpc.stub.StreamObserver<pandora.HostProto.ReadLocalAddressResponse>) responseObserver);
          break;
        case METHODID_CONNECT:
          serviceImpl.connect((pandora.HostProto.ConnectRequest) request,
              (io.grpc.stub.StreamObserver<pandora.HostProto.ConnectResponse>) responseObserver);
          break;
        case METHODID_GET_CONNECTION:
          serviceImpl.getConnection((pandora.HostProto.GetConnectionRequest) request,
              (io.grpc.stub.StreamObserver<pandora.HostProto.GetConnectionResponse>) responseObserver);
          break;
        case METHODID_WAIT_CONNECTION:
          serviceImpl.waitConnection((pandora.HostProto.WaitConnectionRequest) request,
              (io.grpc.stub.StreamObserver<pandora.HostProto.WaitConnectionResponse>) responseObserver);
          break;
        case METHODID_DISCONNECT:
          serviceImpl.disconnect((pandora.HostProto.DisconnectRequest) request,
              (io.grpc.stub.StreamObserver<pandora.HostProto.DisconnectResponse>) responseObserver);
          break;
        case METHODID_CONNECT_LE:
          serviceImpl.connectLE((pandora.HostProto.ConnectLERequest) request,
              (io.grpc.stub.StreamObserver<pandora.HostProto.ConnectLEResponse>) responseObserver);
          break;
        case METHODID_GET_LECONNECTION:
          serviceImpl.getLEConnection((pandora.HostProto.GetLEConnectionRequest) request,
              (io.grpc.stub.StreamObserver<pandora.HostProto.GetLEConnectionResponse>) responseObserver);
          break;
        case METHODID_DISCONNECT_LE:
          serviceImpl.disconnectLE((pandora.HostProto.DisconnectLERequest) request,
              (io.grpc.stub.StreamObserver<com.google.protobuf.Empty>) responseObserver);
          break;
        case METHODID_DELETE_PAIRING:
          serviceImpl.deletePairing((pandora.HostProto.DeletePairingRequest) request,
              (io.grpc.stub.StreamObserver<pandora.HostProto.DeletePairingResponse>) responseObserver);
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
        case METHODID_ON_PAIRING:
          return (io.grpc.stub.StreamObserver<Req>) serviceImpl.onPairing(
              (io.grpc.stub.StreamObserver<pandora.HostProto.PairingEvent>) responseObserver);
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class HostBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    HostBaseDescriptorSupplier() {}

    @java.lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return pandora.HostProto.getDescriptor();
    }

    @java.lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("Host");
    }
  }

  private static final class HostFileDescriptorSupplier
      extends HostBaseDescriptorSupplier {
    HostFileDescriptorSupplier() {}
  }

  private static final class HostMethodDescriptorSupplier
      extends HostBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    HostMethodDescriptorSupplier(String methodName) {
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
      synchronized (HostGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
              .setSchemaDescriptor(new HostFileDescriptorSupplier())
              .addMethod(getResetMethod())
              .addMethod(getReadLocalAddressMethod())
              .addMethod(getConnectMethod())
              .addMethod(getGetConnectionMethod())
              .addMethod(getWaitConnectionMethod())
              .addMethod(getDisconnectMethod())
              .addMethod(getConnectLEMethod())
              .addMethod(getGetLEConnectionMethod())
              .addMethod(getDisconnectLEMethod())
              .addMethod(getOnPairingMethod())
              .addMethod(getDeletePairingMethod())
              .build();
        }
      }
    }
    return result;
  }
}
