"""Render controlled HTTP failures and unexpected server failures consistently."""

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse
from starlette.exceptions import HTTPException

from app.errors import (
    ERRORS,
    ErrorCode,
    HTTPErrorResponse,
    ProtocolError,
    logger,
    validation_summary,
)

HTTP_ERROR_RESPONSES = {
    status: {"model": HTTPErrorResponse, "description": description}
    for status, description in {
        400: "Invalid request",
        404: "Resource not found",
        405: "Method not allowed",
        422: "Request validation failed",
        500: "Unexpected server failure",
        503: "Temporarily unavailable",
    }.items()
}


def register_http_errors(app: FastAPI) -> None:
    @app.exception_handler(ProtocolError)
    async def operation_error(request: Request, exc: ProtocolError) -> JSONResponse:
        return JSONResponse(
            status_code=ERRORS[exc.code].http_status,
            content={"error": exc.report("http").model_dump()},
        )

    @app.exception_handler(HTTPException)
    async def http_error(request: Request, exc: HTTPException) -> JSONResponse:
        code = {
            404: ErrorCode.NOT_FOUND,
            405: ErrorCode.METHOD_NOT_ALLOWED,
            503: ErrorCode.TEMPORARY_UNAVAILABLE,
        }.get(
            exc.status_code,
            ErrorCode.INTERNAL_ERROR if exc.status_code >= 500 else ErrorCode.INVALID_REQUEST,
        )
        failure = ProtocolError(code, f"The HTTP operation returned status {exc.status_code}.")
        return JSONResponse(
            status_code=exc.status_code,
            content={"error": failure.report("http").model_dump()},
            headers=exc.headers,
        )

    @app.exception_handler(RequestValidationError)
    async def validation_error(request: Request, exc: RequestValidationError) -> JSONResponse:
        failure = ProtocolError(ErrorCode.INVALID_REQUEST, validation_summary(exc.errors()))
        return JSONResponse(status_code=422, content={"error": failure.report("http").model_dump()})

    @app.exception_handler(Exception)
    async def unexpected_error(request: Request, exc: Exception) -> JSONResponse:
        error = ProtocolError(ErrorCode.INTERNAL_ERROR).report("http")
        logger.error("requestId=%s unexpected HTTP failure", error.requestId, exc_info=exc)
        return JSONResponse(status_code=500, content={"error": error.model_dump()})
