package codes.thischwa.cf.auth;

public class CfAuthBuilder {

  public static ApiTokenAuth build(String apiToken) {
    return new ApiTokenAuth(apiToken);
  }

  public static EmailKeyAuth build(String authEmail, String authKey) {
    return new EmailKeyAuth(authEmail, authKey);
  }
}
