package ie.flavorfeedback;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.*;
import com.amazon.ask.request.Predicates;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class AddProductHandler implements com.amazon.ask.dispatcher.request.handler.RequestHandler {

    private final AmazonDynamoDB client;
    private final DynamoDB dynamoDB;
    private final String tableName;
    private final Set<String> validCategories = Set.of("food", "beverage", "snack"); // Example categories

    public AddProductHandler() {
        client = AmazonDynamoDBClientBuilder.standard().build();
        dynamoDB = new DynamoDB(client);
        tableName = "products1";
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(Predicates.intentName("AddProductIntent"));
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

            // Extract values from slots
            String productName = intent.getSlots().get("ProductName").getValue();
            String price = intent.getSlots().get("Price").getValue();
            String yearOfManufacture = intent.getSlots().get("YearOfManufacture").getValue();
            String category = intent.getSlots().get("category").getValue();

            // Validate category
            if (!validCategories.contains(category)) {
                String speechText = "Invalid product category. Please choose from " +
                        String.join(", ", "" + validCategories) + ".";
                return handlerInput.getResponseBuilder()
                        .withSpeech(speechText)
                        .withShouldEndSession(false)
                        .build();
            }

            // Add product to database including the category
            boolean success = addProductToDatabase(productName, price, yearOfManufacture, category);

            String speechText = success ? "Product added successfully." : "Failed to add product. Please try again later.";
            String repromptText = "Is there anything else I can help you with?";

            return handlerInput.getResponseBuilder()
                    .withSpeech(speechText)
                    .withReprompt(repromptText)
                    .withShouldEndSession(false) // Keep the session open
                    .build();
        }catch (Exception e){e.printStackTrace();}
        return null;
    }

    private boolean addProductToDatabase(String productName, String price, String yearOfManufacture, String category) {
        try {
            Table table = dynamoDB.getTable(tableName);
            String productId = UUID.randomUUID().toString();

            Item item = new Item()
                    .withPrimaryKey("product_id", productId)
                    .withString("name", productName)
                    .withString("price", price)
                    .withString("year_of_manufacture", yearOfManufacture)
                    .withString("category", category); // Add category to item

            table.putItem(item);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
