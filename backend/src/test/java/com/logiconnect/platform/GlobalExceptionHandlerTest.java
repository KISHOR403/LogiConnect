package com.logiconnect.platform;

import com.logiconnect.platform.common.exception.BadRequestException;
import com.logiconnect.platform.common.exception.ConflictException;
import com.logiconnect.platform.common.exception.ForbiddenException;
import com.logiconnect.platform.common.exception.ResourceNotFoundException;
import com.logiconnect.platform.common.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false) // Disable security filters to isolate exception handler testing
@ActiveProfiles("test")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    public static class TestValidationDto {
        @NotBlank(message = "Name must not be blank")
        @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
        private String name;

        @NotNull(message = "Email must not be null")
        @Email(message = "Email must be a valid email format")
        private String email;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    @TestConfiguration
    @RestController
    @RequestMapping("/test/exceptions")
    public static class TestExceptionController {

        @GetMapping("/not-found")
        public void throwNotFound() {
            throw new ResourceNotFoundException("Employee", "id", "123e4567-e89b-12d3-a456-426614174000");
        }

        @GetMapping("/bad-request")
        public void throwBadRequest() {
            throw new BadRequestException("Invalid date range provided");
        }

        @GetMapping("/forbidden")
        public void throwForbidden() {
            throw new ForbiddenException("Cannot modify employee in another department");
        }

        @GetMapping("/conflict")
        public void throwConflict() {
            throw new ConflictException("Employee code 'EMP001' already exists");
        }

        @PostMapping("/validate")
        public ApiResponse<String> testValidation(@Valid @RequestBody TestValidationDto dto) {
            return ApiResponse.success("Valid DTO received");
        }
    }

    @Test
    @DisplayName("ResourceNotFoundException returns 404 with RESOURCE_NOT_FOUND error code")
    void testResourceNotFoundException() throws Exception {
        mockMvc.perform(get("/test/exceptions/not-found").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.error.message", is("Employee was not found with id: '123e4567-e89b-12d3-a456-426614174000'")));
    }

    @Test
    @DisplayName("BadRequestException returns 400 with BAD_REQUEST error code")
    void testBadRequestException() throws Exception {
        mockMvc.perform(get("/test/exceptions/bad-request").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("BAD_REQUEST")))
                .andExpect(jsonPath("$.error.message", is("Invalid date range provided")));
    }

    @Test
    @DisplayName("ForbiddenException returns 403 with FORBIDDEN error code")
    void testForbiddenException() throws Exception {
        mockMvc.perform(get("/test/exceptions/forbidden").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("FORBIDDEN")))
                .andExpect(jsonPath("$.error.message", is("Cannot modify employee in another department")));
    }

    @Test
    @DisplayName("ConflictException returns 409 with CONFLICT error code")
    void testConflictException() throws Exception {
        mockMvc.perform(get("/test/exceptions/conflict").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("CONFLICT")))
                .andExpect(jsonPath("$.error.message", is("Employee code 'EMP001' already exists")));
    }

    @Test
    @DisplayName("Validation failure returns 400 with field details in ApiError structure")
    void testValidationFailure() throws Exception {
        String invalidJson = """
                {
                    "name": "",
                    "email": "invalid-email"
                }
                """;

        mockMvc.perform(post("/test/exceptions/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success", is(false)))
                .andExpect(jsonPath("$.error.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.error.details", hasSize(3)));
    }
}
