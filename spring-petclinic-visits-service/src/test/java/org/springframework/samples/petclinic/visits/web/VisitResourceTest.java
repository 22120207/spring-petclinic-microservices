package org.springframework.samples.petclinic.visits.web;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.samples.petclinic.visits.model.Visit;
import org.springframework.samples.petclinic.visits.model.VisitRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static java.util.Arrays.asList;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Date;
import java.time.LocalDateTime;
import java.util.TimeZone;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(SpringExtension.class)
@WebMvcTest(VisitResource.class)
@ActiveProfiles("test")
class VisitResourceTest {

    @Autowired
    MockMvc mvc;

    @MockBean
    VisitRepository visitRepository;

    @BeforeAll
    static void setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Test
    void shouldFetchVisits() throws Exception {
        given(visitRepository.findByPetIdIn(asList(111, 222)))
                .willReturn(
                        asList(
                                Visit.VisitBuilder.aVisit()
                                        .id(1)
                                        .petId(111)
                                        .build(),
                                Visit.VisitBuilder.aVisit()
                                        .id(2)
                                        .petId(222)
                                        .build(),
                                Visit.VisitBuilder.aVisit()
                                        .id(3)
                                        .petId(222)
                                        .build()));

        mvc.perform(get("/pets/visits?petId=111,222"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(1))
                .andExpect(jsonPath("$.items[1].id").value(2))
                .andExpect(jsonPath("$.items[2].id").value(3))
                .andExpect(jsonPath("$.items[0].petId").value(111))
                .andExpect(jsonPath("$.items[1].petId").value(222))
                .andExpect(jsonPath("$.items[2].petId").value(222));
    }

    @Test
    void shouldFetchVisitsForSinglePet() throws Exception {
        given(visitRepository.findByPetId(111))
                .willReturn(
                        asList(
                                Visit.VisitBuilder.aVisit()
                                        .id(1)
                                        .petId(111)
                                        .build(),
                                Visit.VisitBuilder.aVisit()
                                        .id(2)
                                        .petId(111)
                                        .build()));

        mvc.perform(get("/owners/*/pets/111/visits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[0].petId").value(111))
                .andExpect(jsonPath("$[1].petId").value(111));
    }

    @Test
    void shouldCreateVisit() throws Exception {
        Visit visit = Visit.VisitBuilder.aVisit()
                .id(1)
                .petId(111)
                .build();

        given(visitRepository.save(any(Visit.class))).willReturn(visit);

        mvc.perform(post("/owners/*/pets/111/visits")
                .contentType("application/json")
                .content("{\"id\":1,\"petId\":111}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.petId").value(111));
    }

    @Test
    void shouldReturnBadRequestForInvalidPetId() throws Exception {
        mvc.perform(post("/owners/*/pets/0/visits")
                .contentType("application/json")
                .content("{\"id\":1,\"petId\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldCreateVisitWithDateAndDescription() throws Exception {
        Visit visit = Visit.VisitBuilder.aVisit()
                .id(1)
                .petId(111)
                .date(java.sql.Date.valueOf(java.time.LocalDate.of(2023, 10, 1))) // Use LocalDate for clarity
                .description("Regular check-up")
                .build();

        given(visitRepository.save(any(Visit.class))).willReturn(visit);

        mvc.perform(post("/owners/*/pets/111/visits")
                .contentType("application/json")
                .content("{\"id\":1,\"petId\":111,\"date\":\"2023-10-01\",\"description\":\"Regular check-up\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.petId").value(111))
                .andExpect(jsonPath("$.date").value("2023-10-01"))
                .andExpect(jsonPath("$.description").value("Regular check-up"));
    }

    @Test
    void shouldFetchVisitsWithDateAndDescription() throws Exception {
        given(visitRepository.findByPetId(111))
                .willReturn(
                        asList(
                                Visit.VisitBuilder.aVisit()
                                        .id(1)
                                        .petId(111)
                                        .date(java.sql.Date.valueOf(java.time.LocalDate.of(2023, 10, 1))) 
                                        .description("Regular check-up")
                                        .build(),
                                Visit.VisitBuilder.aVisit()
                                        .id(2)
                                        .petId(111)
                                        .date(java.sql.Date.valueOf(java.time.LocalDate.of(2023, 10, 2)))
                                        .description("Vaccination")
                                        .build()));

        mvc.perform(get("/owners/*/pets/111/visits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[0].petId").value(111))
                .andExpect(jsonPath("$[1].petId").value(111))
                .andExpect(jsonPath("$[0].date").value("2023-10-01")) // Ensure the expected date matches
                .andExpect(jsonPath("$[1].date").value("2023-10-02"))
                .andExpect(jsonPath("$[0].description").value("Regular check-up"))
                .andExpect(jsonPath("$[1].description").value("Vaccination"));
    }

    
}