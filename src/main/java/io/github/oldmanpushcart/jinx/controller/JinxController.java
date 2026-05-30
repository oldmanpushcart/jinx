package io.github.oldmanpushcart.jinx.controller;

import io.github.oldmanpushcart.jinx.Constants;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/api")
public class JinxController {

    @Get(uri = "/health", produces = MediaType.TEXT_PLAIN)
    public String health() {
        return "OK";
    }

    @Get(uri = "/version", produces = MediaType.TEXT_PLAIN)
    public String version() {
        return Constants.VERSION;
    }

    @Get(uri = "/help", produces = MediaType.TEXT_PLAIN)
    public String help() {
        return """
                """;
    }

}
