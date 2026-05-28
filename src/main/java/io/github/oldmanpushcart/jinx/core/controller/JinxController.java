package io.github.oldmanpushcart.jinx.core.controller;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/api/v1/agent")
public class JinxController {

    @Get("/health")
    public HttpResponse<String> health() {
        return HttpResponse.ok("OK");
    }

    @Get(uri = "/help", produces = MediaType.TEXT_PLAIN)
    public HttpResponse<String> help() {
        return HttpResponse.ok("""
                HELP
                """);
    }
}
