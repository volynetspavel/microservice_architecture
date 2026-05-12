package com.microservice.resource.contract;

import com.microservice.resource.controller.ResourceController;
import com.microservice.resource.dto.ResourceDataResponseDto;
import com.microservice.resource.service.ResourceService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Base class for Spring Cloud Contract provider verifier tests for HTTP contracts.
 *
 * ResourceController uses constructor injection, so no Spring context is needed —
 * a plain Mockito mock is wired directly into the controller via standalone MockMvc setup.
 * The generated test class (ResourcesTest) extends this class automatically.
 */
public abstract class ResourceServiceHttpContractBase {

    @BeforeEach
    void setup() {
        ResourceService mockService = mock(ResourceService.class);
        when(mockService.getResourceById("1"))
                .thenReturn(new ResourceDataResponseDto(new byte[]{(byte) 0xFF, (byte) 0xFB, 0x00}));

        RestAssuredMockMvc.standaloneSetup(new ResourceController(mockService));
    }
}