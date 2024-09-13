package ie.flavorfeedback;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.model.Slot;
import com.amazon.ask.request.Predicates;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import java.util.Optional;
import java.util.UUID;

public class ProductFeedbackHandler implements com.amazon.ask.dispatcher.request.handler.RequestHandler {

    private final DynamoDB dynamoDB;
    private final String tempStorageTableName = "TempProducts";

    public ProductFeedbackHandler() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        this.dynamoDB = new DynamoDB(client);
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(Predicates.intentName("ProductFeedbackIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
       try {
            IntentRequest intentRequest = (IntentRequest) handlerInput.getRequestEnvelope().getRequest();

            Intent intent = intentRequest.getIntent();

            // Check if all slots are filled
            boolean allSlotsFilled = intent.getSlots().values().stream()
                    .allMatch(slot -> slot.getValue() != null);

            if (!allSlotsFilled) {
                // Not all slots are filled. Delegate slot collection back to Alexa.
                return handlerInput.getResponseBuilder()
                        .addDelegateDirective(intent) // Delegate back to Alexa to ask for the next slot
                        .build();
            }

            System.out.println("BEFORE GET SLOT VALUE");
            String productName = intent.getSlots().get("ProductName").getValue();

            System.out.println("BEFORE GET CATEGORY");
            String category = getProductCategory(productName);

            System.out.println("AFTER GET CATEGORY");
            // Save product name and category to a temporary DynamoDB table for confirmation
            System.out.println("BEFORE SAVE TO TEMP");
            saveProductCategoryForConfirmation(productName, category);

            // Ask the user to confirm the category
            String confirmationPrompt = "Please confirm the category followed by the word 'feedback', for example, 'beverage feedback', or 'snack feedback'.";
            return handlerInput.getResponseBuilder()
                    .withSpeech(confirmationPrompt)
                    .withShouldEndSession(false)
                    .build();
        } catch (Exception e) {
            e.printStackTrace();
            return handlerInput.getResponseBuilder()
                    .withSpeech("An error occurred while processing your request. Please try again.")
                    .withShouldEndSession(false)
                    .build();
        }
    }

    private boolean saveProductCategoryForConfirmation(String productName, String category) {
        try {
            Table table = dynamoDB.getTable(tempStorageTableName);
            Item item = new Item()
                    .withPrimaryKey("ID", UUID.randomUUID().toString()) // Assuming a UUID for simplicity
                    .withString("ProductName", productName)
                    .withString("Category", category);

            table.putItem(item);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getProductCategory(String productName) {
        Table table = dynamoDB.getTable("products1");
        Item item = table.getItem("name", productName); // Ensure table and key are correctly configured
        return item != null ? item.getString("category") : "unknown";
    }
}
