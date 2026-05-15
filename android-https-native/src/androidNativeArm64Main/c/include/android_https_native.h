#ifndef ANDROID_HTTPS_NATIVE_H
#define ANDROID_HTTPS_NATIVE_H

#include <stddef.h>
#include <stdint.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef struct acidify_http_header {
    const char *name;
    const char *value;
} acidify_http_header;

typedef struct acidify_http_request {
    const char *method;
    const char *url;
    const acidify_http_header *headers;
    size_t header_count;
    const uint8_t *body;
    size_t body_len;
    const char *content_type;
    int timeout_ms;
    const char *ca_bundle_path;
} acidify_http_request;

typedef struct acidify_http_response {
    uint8_t *raw_response;
    size_t raw_response_len;
    char *error_message;
} acidify_http_response;

int acidify_http_execute(const acidify_http_request *request, acidify_http_response *response);
void acidify_http_response_free(acidify_http_response *response);

#ifdef __cplusplus
}
#endif

#endif
