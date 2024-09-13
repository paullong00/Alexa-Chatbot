package ie.flavorfeedback;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.Response;
import com.amazon.ask.request.Predicates;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.*;
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class GetProductHandler implements com.amazon.ask.dispatcher.request.handler.RequestHandler {

    private final AmazonDynamoDB client;
    private final DynamoDB dynamoDB;

    public GetProductHandler() {
        client = AmazonDynamoDBClientBuilder.standard().build();
        dynamoDB = new DynamoDB(client);
    }

    @Override
    public boolean canHandle(HandlerInput handlerInput) {
        return handlerInput.matches(Predicates.intentName("GetProductIntent"));
    }

    @Override
    public Optional<Response> handle(HandlerInput handlerInput) {
        List<String> products = getProductsFromDynamoDB();
        String speechText = "Here is the list of products: " + String.join(", ", products);
        String repromptText = "Is there anything else I can help you with?";
        return handlerInput.getResponseBuilder()
                .withSpeech(speechText)
                .withReprompt(repromptText)
                .withShouldEndSession(false)
                .build();
    }

    private List<String> getProductsFromDynamoDB() {
        List<String> products = new ArrayList<>();
        Table table = dynamoDB.getTable("products1");

        ScanSpec scanSpec = new ScanSpec();
        try {
            ItemCollection<ScanOutcome> outcome = table.scan(scanSpec);
            Iterator<Item> iterator = outcome.iterator();
            while (iterator.hasNext()) {
                Item item = iterator.next();

                String productName = item.getString("name");
                products.add(productName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return products;
    }
}
