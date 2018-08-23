package ru.sbtqa.tag.api.entries.methods;

import ru.sbtqa.tag.api.EndpointEntry;
import ru.sbtqa.tag.api.Rest;
import ru.sbtqa.tag.api.annotation.Body;
import ru.sbtqa.tag.api.annotation.Endpoint;
import ru.sbtqa.tag.api.annotation.Header;
import ru.sbtqa.tag.api.annotation.Validation;
import ru.sbtqa.tag.api.utils.Default;

import static org.hamcrest.Matchers.equalTo;

@Endpoint(method = Rest.POST, path = "client/post", title = "post with json", template = "templates/Client.json")
public class PostEndpointEntry extends EndpointEntry {

    @Header(name = "Content-Type")
    private String header = "application/json";

    @Body(name = "id")
    private String id = String.valueOf(Default.ID);

    @Body(name = "name")
    private String name = Default.NAME;

    @Body(name = "email")
    private String email = Default.EMAIL;

    @Body(name = "day1")
    String day1 = "false";
    @Body(name = "day2")
    String day2 = "false";
    @Body(name = "day11")
    String day11 = "true";
    @Body(name = "day12")
    String day12 = "true";

    @Validation(title = "result")
    public void validate() {
        getResponse().body("result", equalTo(id + name + email));
    }
}
