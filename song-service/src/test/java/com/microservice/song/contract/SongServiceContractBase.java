package com.microservice.song.contract;

import com.microservice.song.controller.SongController;
import com.microservice.song.dto.DeleteSongsResponseDto;
import com.microservice.song.dto.SongIdResponseDto;
import com.microservice.song.exception.GlobalExceptionHandler;
import com.microservice.song.service.SongService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

/**
 * Base class for Spring Cloud Contract provider verifier tests.
 *
 * SongController uses @Autowired field injection so a Spring slice context is required.
 * Only the controller and its advice are loaded — no JPA, no Eureka, no DB.
 * The generated test classes (SongsTest) extend this class automatically.
 */
@SpringBootTest(
        classes = {SongController.class, GlobalExceptionHandler.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@TestPropertySource(properties = {
        "eureka.client.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.autoconfigure.exclude=" +
                "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration"
})
public abstract class SongServiceContractBase {

    @Autowired
    SongController songController;

    @MockitoBean
    SongService songService;

    @BeforeEach
    void setup() {
        when(songService.deleteSongs("1"))
                .thenReturn(new DeleteSongsResponseDto(List.of(1)));

        when(songService.createSong(argThat(dto -> dto != null && dto.getId() == 5)))
                .thenReturn(new SongIdResponseDto(5));

        RestAssuredMockMvc.standaloneSetup(songController);
    }
}