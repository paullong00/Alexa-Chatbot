package ie.flavorfeedback.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import ie.flavorfeedback.CategoryFeedbackHandler;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SnackCategoryHandler implements CategoryFeedbackHandler {
    private final DynamoDB dynamoDB;
    private final String feedbackTableName = "SnackFeedback";

    public SnackCategoryHandler(DynamoDB dynamoDB) {
        this.dynamoDB = dynamoDB;
    }

    @Override
    public Optional<Response> handleFeedback(String productName, HandlerInput handlerInput) {
        IntentRequest intentRequest = (IntentRequest) handlerInput.getRequestEnvelope().getRequest();
        Intent intent = intentRequest.getIntent();

        Map<String, Slot> slots = intentRequest.getIntent().getSlots();

        // Check if all slots are filled
        boolean allSlotsFilled = intent.getSlots().values().stream()
                .allMatch(slot -> slot.getValue() != null);

        if (!allSlotsFilled) {
            // Not all slots are filled. Delegate slot collection back to Alexa.
            return handlerInput.getResponseBuilder()
                    .addDelegateDirective(intent) // Delegate back to Alexa to ask for the next slot
                    .build();
        }

        // Extracting individual slot values
        String taste = slots.containsKey("taste") ? slots.get("taste").getValue() : "Not provided";
        String bouquet = slots.containsKey("bouquet") ? slots.get("bouquet").getValue() : "Not provided";
        String consistency = slots.containsKey("consistency") ? slots.get("consistency").getValue() : "Not provided";

        // Process and store the feedback
        storeFeedback(productName, taste, bouquet, consistency);

        String speechText = "Thank you for your feedback on the snack.";
        return handlerInput.getResponseBuilder()
                .withSpeech(speechText)
                .withShouldEndSession(true)
                .build();
    }


    private void storeFeedback(String productName, String taste, String bouquet, String consistency) {
        Table table = dynamoDB.getTable(feedbackTableName);
        Item item = new Item()
                .withPrimaryKey("feedback_id", UUID.randomUUID().toString())
                .withString("product_name", productName)
                .withString("taste", taste)
                .withString("bouquet", bouquet)
                .withString("consistency", consistency);

        table.putItem(item);
    }
}
