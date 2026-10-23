package com.botdarr.connections;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.mockserver.junit.MockServerRule;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.HttpResponse;
import org.mockserver.model.MediaType;

import java.nio.charset.StandardCharsets;

public class ConnectionHelperTests {

  @Rule
  public MockServerRule mockServerRule = new MockServerRule(this);

  @Test
  public void makeRequest_treats201CreatedAsSuccess() throws Exception {
    // mattermost (and the arrs) return 201 Created for successful posts
    mockServerRule.getClient()
      .when(HttpRequest.request().withMethod("POST").withPath("/api/v4/posts"))
      .respond(HttpResponse.response()
        .withStatusCode(201)
        .withBody("{\"id\":\"new-post-id\"}", MediaType.APPLICATION_JSON));

    String body = ConnectionHelper.makeRequest(postRequest("/api/v4/posts"),
      new ConnectionHelper.SimpleEntityResponseHandler<String>() {
        @Override
        public String onSuccess(String response) {
          return response;
        }
      });

    Assert.assertEquals("{\"id\":\"new-post-id\"}", body);
  }

  @Test
  public void makeRequest_treats404AsFailure() {
    mockServerRule.getClient()
      .when(HttpRequest.request().withMethod("POST").withPath("/missing"))
      .respond(HttpResponse.response().withStatusCode(404));

    String body = ConnectionHelper.makeRequest(postRequest("/missing"),
      new ConnectionHelper.SimpleEntityResponseHandler<String>() {
        @Override
        public String onSuccess(String response) {
          return response;
        }
      });

    Assert.assertNull(body);
  }

  private ConnectionHelper.RequestHandler postRequest(String path) {
    return new ConnectionHelper.RequestHandler() {
      @Override
      public HttpPost buildRequest() {
        HttpPost post = new HttpPost("http://localhost:" + mockServerRule.getPort() + path);
        post.addHeader("content-type", "application/json");
        post.setEntity(new StringEntity("{}", StandardCharsets.UTF_8));
        return post;
      }
    };
  }
}
