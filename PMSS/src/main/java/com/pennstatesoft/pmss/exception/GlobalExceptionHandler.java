package com.pennstatesoft.pmss.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Global exception handling for every controller. Any exception that escapes a
 * handler method is caught here, logged, and rendered as a friendly error page
 * (templates/error.html) instead of a stack trace / whitelabel page
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String ERROR_STRING = "error";
    private static final String MESSAGE_STRING = "message";
    private static final String STATUS_STRING = "status";

    /** Missing DB row — e.g. acting on a meeting/room/user id that no longer exists. */
    @ExceptionHandler(EmptyResultDataAccessException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNotFound(EmptyResultDataAccessException ex, Model model) {
        log.warn("Resource not found: {}", ex.getMessage());
        model.addAttribute(STATUS_STRING, 404);
        model.addAttribute(MESSAGE_STRING, "The item you requested could not be found. It may have been deleted.");
        return ERROR_STRING;
    }

    /** Malformed input — bad path variable type, unparseable date, etc. */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleBadRequest(Exception ex, Model model) {
        log.warn("Bad request", ex);
        model.addAttribute(STATUS_STRING, 400);
        model.addAttribute(MESSAGE_STRING, "That request was not valid. Please check your input and try again.");
        return ERROR_STRING;
    }

    /** Everything else */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpected(Exception ex, Model model) {
        log.error("Unhandled exception", ex);
        model.addAttribute(STATUS_STRING, 500);
        model.addAttribute(MESSAGE_STRING, "Something went wrong on our end. Please try again in a moment.");
        return ERROR_STRING;
    }
}
