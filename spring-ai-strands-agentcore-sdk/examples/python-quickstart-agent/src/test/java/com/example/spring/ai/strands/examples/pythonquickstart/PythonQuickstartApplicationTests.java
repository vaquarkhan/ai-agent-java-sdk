package com.example.spring.ai.strands.examples.pythonquickstart;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * @author Vaquar Khan
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PythonQuickstartApplicationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void quickstartEndpointReturnsJson() {
        var body = restTemplate.getForObject(
                "http://localhost:" + port + "/api/quickstart", PythonQuickstartController.QuickstartHttpResponse.class);
        assertThat(body).isNotNull();
        assertThat(body.content()).isNotBlank();
        assertThat(body.iterationCount()).isPositive();
    }
}
