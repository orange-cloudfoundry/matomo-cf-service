/**
 * Orange File HEADER
 */
package com.orange.oss.matomocfservice.api;

import javax.persistence.EntityNotFoundException;
import javax.validation.ConstraintViolationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.orange.oss.matomocfservice.api.model.Error;
import com.orange.oss.matomocfservice.api.model.ErrorCode;

/**
 * @author P. Déchamboux
 *
 */
public class PlatformApiExceptionHandler extends ResponseEntityExceptionHandler {
	private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());
	
	// when trying to get a non existing resource 
	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public final Error handleIllegalArgumentException(IllegalArgumentException e) { 
        return createError(ErrorCode.UNKNOWN_RESOURCE, "Wrong input parameter.", e, HttpStatus.NOT_FOUND);
	}
	
	// when trying to delete a non existing resource
	@ExceptionHandler(EmptyResultDataAccessException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public final Error handleEmptyResultException(EmptyResultDataAccessException e) { 
        return createError(ErrorCode.UNKNOWN_RESOURCE, "Resource does not exist.", e, HttpStatus.NOT_FOUND);
	}
	
	// when trying to delete a non existing resource
	@ExceptionHandler(EntityNotFoundException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public final Error handleEmptyResultException(EntityNotFoundException e) { 
        return createError(ErrorCode.UNKNOWN_RESOURCE, "Resource does not exist.", e, HttpStatus.NOT_FOUND);
	}

	// when trying to create an empty resource
	@ExceptionHandler(ConstraintViolationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public final Error handleConstraintViolationException(ConstraintViolationException e) { 
        return createError(ErrorCode.INVALID_INPUT, "Resource with null or empty values.", e, HttpStatus.BAD_REQUEST);
	}

	// when trying to use a wrong transaction
	@ExceptionHandler(TransactionSystemException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public final Error handleConstraintViolationException(TransactionSystemException e) { 
		return createError(ErrorCode.TRANSACTION_ISSUE, "Problem with transaction.", e, HttpStatus.BAD_REQUEST);
	}

	// when trying to create a duplicated resource
	@ExceptionHandler(DataIntegrityViolationException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public final Error handleDataIntegrityViolationException(DataIntegrityViolationException e) { 
        return createError(ErrorCode.DUPLICATED_RESOURCE, "Resource already exists.", e, HttpStatus.BAD_REQUEST);
	}
	
	// for all other exceptions
	@ExceptionHandler
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public final Error handleAllException(Exception e) {
		return createError(ErrorCode.TECHNICAL_ERROR, "Service is unavailable.", e, HttpStatus.INTERNAL_SERVER_ERROR);
	}

	private Error createError(ErrorCode errorCode, String message, Exception e, HttpStatus status){
		Error error = new Error();
		error.setCode(errorCode);
		error.setMessage(message);
		error.setDescription(e.getMessage());
		LOGGER.debug("Handle " + e.getClass().getName() + " (status=" + status + "): " + error.toString());
		return error;
	}
}
