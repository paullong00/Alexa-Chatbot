package ie.flavorfeedback;

import com.amazon.ask.Skill;
import com.amazon.ask.builder.StandardSkillBuilder;
import com.amazon.ask.model.RequestEnvelope;
import com.amazon.ask.model.ResponseEnvelope;
//import com.amazon.ask.util.JacksonSerializer;
import com.amazon.ask.model.services.util.JacksonSerializer;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.util.IOUtils;
import ie.flavorfeedback.handlers.BeverageCategoryHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class FlavorFeedbackHandler implements RequestStreamHandler {

    private final Skill skill;
    private final JacksonSerializer serializer;

    public FlavorFeedbackHandler() {
        skill = new StandardSkillBuilder()
                .addRequestHandler(new GetProductHandler())
                .addRequestHandler(new LaunchRequestHandler())
                .addRequestHandler(new AddProductHandler())
                .addRequestHandler(new ExitIntentHandler())
                .addRequestHandler(new ProductFeedbackHandler())
                .addRequestHandler(new BeverageCategoryHandler())
                .build();
        serializer = new JacksonSerializer();
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        System.out.println("\n\n\nFLAVOUR  HANDLER ");
        String request = IOUtils.toString(inputStream);
        try {
            RequestEnvelope requestEnvelope = serializer.deserialize(request, RequestEnvelope.class);
            ResponseEnvelope responseEnvelope = skill.invoke(requestEnvelope);
            byte[] response = serializer.serialize(responseEnvelope).getBytes(StandardCharsets.UTF_8);
            outputStream.write(response);
        }catch(Exception e){e.printStackTrace();}
    }
}
