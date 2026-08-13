package com.pennstatesoft.pmss.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void notFoundMapsTo404ErrorPage() {
        Model model = new ExtendedModelMap();

        String view = handler.handleNotFound(new EmptyResultDataAccessException(1), model);

        assertEquals("error", view);
        assertEquals(404, model.getAttribute("status"));
        assertEquals("The item you requested could not be found. It may have been deleted.",
                model.getAttribute("message"));
    }

    @Test
    void badRequestMapsTo400ErrorPage() {
        Model model = new ExtendedModelMap();

        String view = handler.handleBadRequest(new IllegalArgumentException("bad"), model);

        assertEquals("error", view);
        assertEquals(400, model.getAttribute("status"));
    }

    @Test
    void unexpectedMapsTo500ErrorPage() {
        Model model = new ExtendedModelMap();

        String view = handler.handleUnexpected(new RuntimeException("boom"), model);

        assertEquals("error", view);
        assertEquals(500, model.getAttribute("status"));
    }
}
