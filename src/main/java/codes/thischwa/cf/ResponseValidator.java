package codes.thischwa.cf;

import codes.thischwa.cf.model.AbstractResponse;
import codes.thischwa.cf.model.AbstractSingleResponse;
import codes.thischwa.cf.model.RecordMultipleResponse;
import codes.thischwa.cf.model.ResponseResultInfo;
import java.util.stream.Collectors;

/**
 * Validates API responses to ensure compliance with expected conditions, such as response success
 * and result count.
 *
 * <p>This class performs two primary validation tasks:
 * <ul>
 * <li>It checks whether the API response was successful by analyzing the associated response
 * metadata. If the response indicates failure, an exception is thrown with descriptive error
 * messages.
 * <li>It validates the number of results in the API response payload to detect unexpected counts.
 * For {@link RecordMultipleResponse}, it checks if results are empty or if more than one result
 * was returned when a single result was expected. For {@link AbstractSingleResponse}, it checks
 * if the result is null. Depending on the parameter 'emptyResultThrowsException', an exception
 * will be triggered or an empty/null result will be returned.
 * </ul>
 */
class ResponseValidator {
  private final boolean emptyResultThrowsException;

  ResponseValidator(boolean emptyResultThrowsException) {
    this.emptyResultThrowsException = emptyResultThrowsException;
  }

  void validate(AbstractResponse resp, boolean singleResultExpected) throws CloudflareApiException {
    validateResponseSuccess(resp);
    validateResultCount(resp, singleResultExpected);
  }

  private void validateResponseSuccess(AbstractResponse resp) throws CloudflareApiException {
    ResponseResultInfo resultInfo = resp.getResponseResultInfo();
    if (!resultInfo.isSuccess()) {
      String errors =
          resultInfo.getErrors().stream().map(Object::toString).collect(Collectors.joining(", "));
      throw new CloudflareApiException("Error in response: " + errors);
    }
  }

  private void validateResultCount(AbstractResponse resp, boolean singleResultExpected)
      throws CloudflareApiException {
    if (resp instanceof RecordMultipleResponse respMulti) {
      if (singleResultExpected && respMulti.getResultInfo().totalCount() > 1) {
        throw new CloudflareApiException(
            "Unexpected result count: " + respMulti.getResultInfo().totalCount());
      }
      if (emptyResultThrowsException && respMulti.getResultInfo().totalCount() == 0) {
        throw new CloudflareNotFoundException("No result found");
      }
    } else if (resp instanceof AbstractSingleResponse<?> respSingle) {
      if (emptyResultThrowsException && respSingle.getResult() == null) {
        throw new CloudflareNotFoundException("No result found");
      }
    }
  }

}
