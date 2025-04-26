package codes.thischwa.cf;

import codes.thischwa.cf.model.AbstractResponse;
import codes.thischwa.cf.model.RecordMultipleResponse;
import codes.thischwa.cf.model.ResponseResultInfo;
import codes.thischwa.cf.model.ResultInfo;
import java.util.Arrays;
import lombok.Getter;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResponseValidatorTest {

  @Mock
  private AbstractResponse mockResponse;

  @Mock
  private ResponseResultInfo mockResultInfo;

  @Mock
  private RecordMultipleResponse mockMultipleResponse;

  private ResponseValidator validatorWithException;
  private ResponseValidator validatorWithoutException;

  @BeforeEach
  void setUp() {
    validatorWithException = new ResponseValidator(true);
    validatorWithoutException = new ResponseValidator(false);
  }

  @Test
  void validateSuccessfulResponse() {
    when(mockResponse.getResponseResultInfo()).thenReturn(mockResultInfo);
    when(mockResultInfo.isSuccess()).thenReturn(true);

    assertDoesNotThrow(() -> validatorWithException.validate(mockResponse, false));
  }

  @Test
  void validateFailedResponse() {
    when(mockResponse.getResponseResultInfo()).thenReturn(mockResultInfo);
    when(mockResultInfo.isSuccess()).thenReturn(false);
    when(mockResultInfo.getErrors()).thenReturn(Arrays.asList("Fehler 1", "Fehler 2"));

    CloudflareApiException exception = assertThrows(CloudflareApiException.class,
        () -> validatorWithException.validate(mockResponse, false));
    assertTrue(exception.getMessage().contains("Fehler 1"));
    assertTrue(exception.getMessage().contains("Fehler 2"));
  }

  @Test
  void validateSingleResultExpectedButMultipleFound() {
    when(mockMultipleResponse.getResponseResultInfo()).thenReturn(mockResultInfo);
    when(mockResultInfo.isSuccess()).thenReturn(true);
    when(mockMultipleResponse.getResultInfo()).thenReturn(new TestResultInfo(2));

    CloudflareApiException exception = assertThrows(CloudflareApiException.class,
        () -> validatorWithException.validate(mockMultipleResponse, true));
    assertTrue(exception.getMessage().contains("Unexpected result count: 2"));
  }

  @Test
  void validateEmptyResultWithExceptionEnabled() {
    when(mockMultipleResponse.getResponseResultInfo()).thenReturn(mockResultInfo);
    when(mockResultInfo.isSuccess()).thenReturn(true);
    when(mockMultipleResponse.getResultInfo()).thenReturn(new TestResultInfo(0));

    assertThrows(CloudflareNotFoundException.class,
        () -> validatorWithException.validate(mockMultipleResponse, false));
  }

  @Test
  void validateEmptyResultWithExceptionDisabled() {
    when(mockMultipleResponse.getResponseResultInfo()).thenReturn(mockResultInfo);
    when(mockResultInfo.isSuccess()).thenReturn(true);
    when(mockMultipleResponse.getResultInfo()).thenReturn(new TestResultInfo(0));

    assertDoesNotThrow(() -> validatorWithoutException.validate(mockMultipleResponse, true));
  }

  @Getter
  private static class TestResultInfo extends ResultInfo {
    private final int totalCount;

    TestResultInfo(int totalCount) {
      this.totalCount = totalCount;
    }
  }
}