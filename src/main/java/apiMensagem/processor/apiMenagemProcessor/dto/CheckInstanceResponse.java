package apiMensagem.processor.apiMenagemProcessor.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckInstanceResponse {
    private InstanceInfo instance;
    private ErrorInfo error;
    private Integer status;
    private ErrorResponse response;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InstanceInfo {
        private String instanceName;
        private String state;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorInfo {
        private String error;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorResponse {
        private String[] message;
    }

    public static CheckInstanceResponse notFound(String instanceName, String responseBody) {
        CheckInstanceResponse resp = new CheckInstanceResponse();
        resp.setStatus(404);
        resp.setError(new ErrorInfo("Not Found"));
        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setMessage(new String[]{"The \"" + instanceName + "\" instance does not exist"});
        resp.setResponse(errorResponse);
        return resp;
    }
}

