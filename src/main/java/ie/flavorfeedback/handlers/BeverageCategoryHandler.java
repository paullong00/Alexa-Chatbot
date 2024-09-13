package ie.flavorfeedback.handlers;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.handler.RequestHandler;
import com.amazon.ask.model.Intent;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PrimaryKey;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.QuerySpec;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;
import com.amazonaws.services.dynamodbv2.document.utils.ValueMap;

import java.util.Iterator;
import java.util.Optional;
import java.util.UUID;

public class BeverageCategoryHandler implements com.amazon.ask.dispatcher.request.handler.RequestHandler {

    private final DynamoDB dynamoDB;
    private final String feedbackTableName = "BeverageFeedback";
    private final String tempStorageTableName = "TempProducts";

    public BeverageCategoryHandler() {
        AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
        this.dynamoDB = new DynamoDB(client);
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(Predicates.intentName("BeverageCategoryIntent"));
    }

    public Optional<Response> handle(HandlerInput handlerInput) {
        Table tempTable = dynamoDB.getTable(tempStorageTableName);

        ScanSpec scanSpec = new ScanSpec().withMaxResultSize(1);
        Iterator<Item> items = tempTable.scan(scanSpec).iterator();

        Item productCategoryItem = null;
        if (items.hasNext()) {
            productCategoryItem = items.next();
        }

        if (productCategoryItem == null) {
            return handlerInput.getResponseBuilder()
                    .withSpeech("I'm sorry, I couldn't find the product information.")
                    .withShouldEndSession(false)
                    .build();
        }

        String productName = productCategoryItem.getString("ProductName");

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

        // Extract values from slots
        String texture = intent.getSlots().get("Texture").getValue();
        String bouquet = intent.getSlots().get("Bouquet").getValue();
        String aftertase = intent.getSlots().get("Aftertase").getValue();

        boolean success = storeFeedback(productName, texture, bouquet, aftertase);

        String speechText = success ? "Feedback recorded successfully." :
                "Failed to record feedback. Please try again later.";
        String repromptText = "Is there anything else I can help you with?";

        tempTable.deleteItem(new PrimaryKey("ID", productCategoryItem.getString("ID")));

        return handlerInput.getResponseBuilder()
                .withSpeech(speechText)
                .withReprompt(repromptText)
                .withShouldEndSession(false)
                .build();
    }

    // Method to store feedback, assuming slots are collected via separate interactions
    public boolean storeFeedback(String productName, String texture, String bouquet, String aftertaste) {
        try {
            Table table = dynamoDB.getTable(feedbackTableName);
            Item item = new Item()
                    .withPrimaryKey("beverageFeedback_id", UUID.randomUUID().toString())
                    .withString("ProductName", productName)
                    .withString("Texture", texture)
                    .withString("Bouquet", bouquet)
                    .withString("Aftertaste", aftertaste);

            table.putItem(item);
            return true;
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
    }

}
