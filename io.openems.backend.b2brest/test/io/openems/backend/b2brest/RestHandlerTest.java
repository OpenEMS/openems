package io.openems.backend.b2brest;

import io.openems.backend.common.metadata.User;
import io.openems.common.exceptions.OpenemsError;
import io.openems.common.exceptions.OpenemsException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(Enclosed.class)
public class RestHandlerTest {

  public static class HandleTests {

    private Backend2BackendRest mockedB2bRest;
    private Request mockedBaseRequest;
    private HttpServletRequest mockedHttpServletRequest;
    private HttpServletResponse mockedHttpServletResponse;

    private User mockedUser;

    @Before
    public void setUp() {
      this.mockedB2bRest = mock(Backend2BackendRest.class);
      this.mockedBaseRequest = mock(Request.class);
      this.mockedHttpServletRequest = mock(HttpServletRequest.class);
      this.mockedHttpServletResponse = mock(HttpServletResponse.class);
      this.mockedUser = mock(User.class);
    }

    @Test
    public void shouldThrowOnEmptyEndpointPath() {
      var sut = new RestHandler(this.mockedB2bRest);

      assertThatExceptionOfType(IOException.class).isThrownBy(
          () -> sut.handle("", this.mockedBaseRequest, this.mockedHttpServletRequest, this.mockedHttpServletResponse)
      );
    }

    @Test
    public void shouldThrowOnRootEndpoint() throws OpenemsError.OpenemsNamedException {
      var sut = spy(new RestHandler(this.mockedB2bRest));
      doReturn(this.mockedUser).when(sut).authenticate(any());

      assertThatExceptionOfType(IOException.class).isThrownBy(
          () -> sut.handle("/", this.mockedBaseRequest, this.mockedHttpServletRequest, this.mockedHttpServletResponse)
      );
    }

    @Test
    public void shouldThrowOtherRootEndpoint() throws OpenemsError.OpenemsNamedException {
      var sut = spy(new RestHandler(this.mockedB2bRest));
      doReturn(this.mockedUser).when(sut).authenticate(any());

      assertThatExceptionOfType(IOException.class).isThrownBy(
          () -> sut.handle("?query=something", this.mockedBaseRequest, this.mockedHttpServletRequest, this.mockedHttpServletResponse)
      );
    }

    @Test
    public void shouldThrowOnUnknownEndpoint() throws OpenemsError.OpenemsNamedException {
      var sut = spy(new RestHandler(this.mockedB2bRest));
      doReturn(this.mockedUser).when(sut).authenticate(any());

      assertThatExceptionOfType(IOException.class).isThrownBy(
          () -> sut.handle("/rubbish", this.mockedBaseRequest, this.mockedHttpServletRequest, this.mockedHttpServletResponse)
      );
    }

    @Test
    public void shouldReturnOkOnUnauthenticatedLiveEndpoint() throws IOException, OpenemsException {
      var sut = spy(new RestHandler(this.mockedB2bRest));
      doNothing().when(sut).sendOkResponse(any(), any(), any());

      sut.handle("/live", this.mockedBaseRequest, this.mockedHttpServletRequest, this.mockedHttpServletResponse);

      verify(sut).sendOkResponse(any(), any(), any());
    }

    @Test
    public void shouldThrowOnUnauthenticatedJsonRpcRequest() throws OpenemsError.OpenemsNamedException {
      var sut = spy(new RestHandler(this.mockedB2bRest));
      var mockedUser = mock(User.class);
      doThrow(OpenemsError.COMMON_AUTHENTICATION_FAILED.exception()).when(sut).authenticate(this.mockedHttpServletRequest);

      assertThatExceptionOfType(IOException.class).isThrownBy(
          () -> sut.handle("/jsonrpc", this.mockedBaseRequest, this.mockedHttpServletRequest, this.mockedHttpServletResponse)
      );

      verify(sut, times(0)).handleJsonRpc(mockedUser, this.mockedBaseRequest, this.mockedHttpServletRequest, this.mockedHttpServletResponse);
    }

    @Test
    public void shouldHandleAuthenticatedJsonRpc() throws IOException, OpenemsError.OpenemsNamedException {
      var sut = spy(new RestHandler(this.mockedB2bRest));
      var mockedUser = mock(User.class);
      doReturn(mockedUser).when(sut).authenticate(any());
      doNothing().when(sut).handleJsonRpc(mockedUser, this.mockedBaseRequest, this.mockedHttpServletRequest, this.mockedHttpServletResponse);

      sut.handle("/jsonrpc", this.mockedBaseRequest, this.mockedHttpServletRequest, this.mockedHttpServletResponse);

      verify(sut).handleJsonRpc(mockedUser, this.mockedBaseRequest, this.mockedHttpServletRequest, this.mockedHttpServletResponse);
    }
  }
}
