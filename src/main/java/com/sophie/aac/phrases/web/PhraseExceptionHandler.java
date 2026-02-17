package com.sophie.aac.phrases.web;

import com.sophie.aac.phrases.domain.PhraseNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class PhraseExceptionHandler {

    @ExceptionHandler(PhraseNotFoundException.class)
    public ProblemDetail handleNotFound(PhraseNotFoundException ex) {
        var pd = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        pd.setTitle("Not Found");
        return pd;
    }
}
