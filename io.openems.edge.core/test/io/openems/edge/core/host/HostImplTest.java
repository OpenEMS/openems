package io.openems.edge.core.host;

/**
 * This is not a real JUnit test, but can be used to mock the JsonRpc-Calls from
 * UI.
 */
public class HostImplTest {

//	private final static User OWNER = new DummyUser("owner", "owner", Role.OWNER);
//
//	@Test
//	public void test() throws OpenemsException, Exception {
//		final DummyConfigurationAdmin cm = new DummyConfigurationAdmin();
//		cm.getOrCreateEmptyConfiguration(Host.SINGLETON_SERVICE_PID);
//		final HostImpl sut = new HostImpl();
//
//		new ComponentTest(sut) //
//				.addReference("cm", cm) //
//				.activate(MyConfig.create() //
//						.setNetworkConfiguration("") //
//						.setUsbConfiguration("") //
//						.build());
//
//		{
//			CompletableFuture<? extends JsonrpcResponseSuccess> future = sut.handleJsonrpcRequest(OWNER,
//					new GetSystemUpdateStateRequest());
//			Thread.sleep(1000);
//			JsonrpcResponseSuccess response = future.get();
//			System.out.println(response.getResult());
//		}
//		{
//			CompletableFuture<? extends JsonrpcResponseSuccess> future = sut.handleJsonrpcRequest(OWNER,
//					new ExecuteSystemUpdateRequest(true));
//			for (int i = 0; i < 2; i++) {
//				Thread.sleep(500);
//				CompletableFuture<? extends JsonrpcResponseSuccess> future2 = sut.handleJsonrpcRequest(OWNER,
//						new GetSystemUpdateStateRequest());
//				JsonrpcResponseSuccess response = future2.get();
//				System.out.println(response.getResult());
//			}
//
//			JsonrpcResponseSuccess response = future.get();
//			System.out.println("FINISHED");
//			JsonUtils.prettyPrint(response.getResult());
//		}
//
//		Thread.sleep(2000);
//		CompletableFuture<? extends JsonrpcResponseSuccess> future2 = sut.handleJsonrpcRequest(OWNER,
//				new GetSystemUpdateStateRequest());
//		JsonrpcResponseSuccess response = future2.get();
//		System.out.println(response.getResult());
//
//		Thread.sleep(10000);
//	}

}
