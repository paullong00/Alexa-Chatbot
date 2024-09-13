package ie.flavorfeedback;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import java.util.Optional;

public interface CategoryFeedbackHandler {
    Optional<Response> handleFeedback(String productName, HandlerInput handlerInput);
}
