#include "android_https_native.h"

#include <errno.h>
#include <netdb.h>
#include <stdbool.h>
#include <stdarg.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <strings.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <unistd.h>

#include <mbedtls/ctr_drbg.h>
#include <mbedtls/entropy.h>
#include <mbedtls/error.h>
#include <mbedtls/net_sockets.h>
#include <mbedtls/ssl.h>
#include <mbedtls/x509_crt.h>

extern const unsigned char acidify_embedded_ca_bundle[];
extern const size_t acidify_embedded_ca_bundle_len;

typedef struct parsed_url {
    bool is_https;
    char *host;
    char *port;
    char *path;
} parsed_url;

typedef struct io_context {
    bool use_tls;
    mbedtls_net_context net;
    mbedtls_ssl_context ssl;
    mbedtls_ssl_config config;
    mbedtls_ctr_drbg_context ctr_drbg;
    mbedtls_entropy_context entropy;
    mbedtls_x509_crt ca_cert;
} io_context;

static char *acidify_strdup(const char *source) {
    if (source == NULL) return NULL;
    size_t length = strlen(source);
    char *copy = (char *) malloc(length + 1);
    if (copy == NULL) return NULL;
    memcpy(copy, source, length + 1);
    return copy;
}

static char *acidify_dup_range(const char *start, const char *end) {
    size_t length = (size_t) (end - start);
    char *copy = (char *) malloc(length + 1);
    if (copy == NULL) return NULL;
    memcpy(copy, start, length);
    copy[length] = '\0';
    return copy;
}

static void set_error(acidify_http_response *response, const char *format, ...) {
    va_list args;
    va_start(args, format);
    int size = vsnprintf(NULL, 0, format, args);
    va_end(args);
    if (size < 0) return;
    response->error_message = (char *) malloc((size_t) size + 1);
    if (response->error_message == NULL) return;
    va_start(args, format);
    vsnprintf(response->error_message, (size_t) size + 1, format, args);
    va_end(args);
}

static void free_parsed_url(parsed_url *url) {
    if (url == NULL) return;
    free(url->host);
    free(url->port);
    free(url->path);
    memset(url, 0, sizeof(*url));
}

static int parse_url(const char *input, parsed_url *output) {
    memset(output, 0, sizeof(*output));
    const char *scheme_end = strstr(input, "://");
    if (scheme_end == NULL) return -1;
    if ((size_t) (scheme_end - input) == 5 && strncasecmp(input, "https", 5) == 0) {
        output->is_https = true;
        output->port = acidify_strdup("443");
    } else if ((size_t) (scheme_end - input) == 4 && strncasecmp(input, "http", 4) == 0) {
        output->is_https = false;
        output->port = acidify_strdup("80");
    } else {
        return -1;
    }
    if (output->port == NULL) return -1;

    const char *authority = scheme_end + 3;
    const char *path = strchr(authority, '/');
    const char *query = strchr(authority, '?');
    const char *fragment = strchr(authority, '#');
    const char *authority_end = authority + strlen(authority);
    if (path != NULL && path < authority_end) authority_end = path;
    if (query != NULL && query < authority_end) authority_end = query;
    if (fragment != NULL && fragment < authority_end) authority_end = fragment;
    const char *port_sep = NULL;
    for (const char *cursor = authority; cursor < authority_end; ++cursor) {
        if (*cursor == ':') {
            port_sep = cursor;
            break;
        }
    }

    if (port_sep != NULL) {
        output->host = acidify_dup_range(authority, port_sep);
        free(output->port);
        output->port = acidify_dup_range(port_sep + 1, authority_end);
    } else {
        output->host = acidify_dup_range(authority, authority_end);
    }
    const char *path_start = path;
    if (path_start == NULL || path_start > authority_end) {
        path_start = query != NULL ? query : fragment;
    }
    output->path = acidify_strdup(path_start != NULL ? path_start : "/");

    if (output->host == NULL || output->port == NULL || output->path == NULL) {
        free_parsed_url(output);
        return -1;
    }
    return 0;
}

static void io_context_init(io_context *context) {
    memset(context, 0, sizeof(*context));
    mbedtls_net_init(&context->net);
    mbedtls_ssl_init(&context->ssl);
    mbedtls_ssl_config_init(&context->config);
    mbedtls_ctr_drbg_init(&context->ctr_drbg);
    mbedtls_entropy_init(&context->entropy);
    mbedtls_x509_crt_init(&context->ca_cert);
}

static void io_context_free(io_context *context) {
    mbedtls_ssl_free(&context->ssl);
    mbedtls_ssl_config_free(&context->config);
    mbedtls_ctr_drbg_free(&context->ctr_drbg);
    mbedtls_entropy_free(&context->entropy);
    mbedtls_x509_crt_free(&context->ca_cert);
    mbedtls_net_free(&context->net);
}

static int set_socket_timeout(int fd, int timeout_ms) {
    if (timeout_ms <= 0) return 0;
    struct timeval timeout;
    timeout.tv_sec = timeout_ms / 1000;
    timeout.tv_usec = (timeout_ms % 1000) * 1000;
    if (setsockopt(fd, SOL_SOCKET, SO_RCVTIMEO, &timeout, sizeof(timeout)) != 0) return -1;
    if (setsockopt(fd, SOL_SOCKET, SO_SNDTIMEO, &timeout, sizeof(timeout)) != 0) return -1;
    return 0;
}

static int connect_io(const parsed_url *url, const acidify_http_request *request, io_context *context, acidify_http_response *response) {
    int ret = mbedtls_net_connect(&context->net, url->host, url->port, MBEDTLS_NET_PROTO_TCP);
    if (ret != 0) {
        set_error(response, "Failed to connect to %s:%s (mbedtls ret=%d)", url->host, url->port, ret);
        return -1;
    }
    if (set_socket_timeout(context->net.fd, request->timeout_ms) != 0) {
        set_error(response, "Failed to configure socket timeout: %s", strerror(errno));
        return -1;
    }

    if (!url->is_https) {
        context->use_tls = false;
        return 0;
    }
    context->use_tls = true;
    const char *personalization = "acidify-android-https";
    if ((ret = mbedtls_ctr_drbg_seed(
            &context->ctr_drbg,
            mbedtls_entropy_func,
            &context->entropy,
            (const unsigned char *) personalization,
            strlen(personalization)
        )) != 0) {
        set_error(response, "mbedtls_ctr_drbg_seed failed: %d", ret);
        return -1;
    }
    if (request->ca_bundle_path != NULL && request->ca_bundle_path[0] != '\0') {
        if ((ret = mbedtls_x509_crt_parse_file(&context->ca_cert, request->ca_bundle_path)) != 0) {
            set_error(response, "mbedtls_x509_crt_parse_file failed: %d", ret);
            return -1;
        }
    } else {
        if ((ret = mbedtls_x509_crt_parse(&context->ca_cert, acidify_embedded_ca_bundle, acidify_embedded_ca_bundle_len)) != 0) {
            set_error(response, "mbedtls_x509_crt_parse (embedded CA) failed: %d", ret);
            return -1;
        }
    }
    if ((ret = mbedtls_ssl_config_defaults(
            &context->config,
            MBEDTLS_SSL_IS_CLIENT,
            MBEDTLS_SSL_TRANSPORT_STREAM,
            MBEDTLS_SSL_PRESET_DEFAULT
        )) != 0) {
        set_error(response, "mbedtls_ssl_config_defaults failed: %d", ret);
        return -1;
    }
    mbedtls_ssl_conf_authmode(&context->config, MBEDTLS_SSL_VERIFY_REQUIRED);
    mbedtls_ssl_conf_ca_chain(&context->config, &context->ca_cert, NULL);
    mbedtls_ssl_conf_rng(&context->config, mbedtls_ctr_drbg_random, &context->ctr_drbg);
    if ((ret = mbedtls_ssl_setup(&context->ssl, &context->config)) != 0) {
        set_error(response, "mbedtls_ssl_setup failed: %d", ret);
        return -1;
    }
    if ((ret = mbedtls_ssl_set_hostname(&context->ssl, url->host)) != 0) {
        set_error(response, "mbedtls_ssl_set_hostname failed: %d", ret);
        return -1;
    }
    mbedtls_ssl_set_bio(&context->ssl, &context->net, mbedtls_net_send, mbedtls_net_recv, NULL);
    do {
        ret = mbedtls_ssl_handshake(&context->ssl);
    } while (ret == MBEDTLS_ERR_SSL_WANT_READ || ret == MBEDTLS_ERR_SSL_WANT_WRITE);
    if (ret != 0) {
        char error_buffer[256];
        mbedtls_strerror(ret, error_buffer, sizeof(error_buffer));
        set_error(response, "mbedtls_ssl_handshake failed: %s", error_buffer);
        return -1;
    }
    uint32_t verify_flags = mbedtls_ssl_get_verify_result(&context->ssl);
    if (verify_flags != 0) {
        char verify_buffer[512];
        mbedtls_x509_crt_verify_info(verify_buffer, sizeof(verify_buffer), "", verify_flags);
        set_error(response, "TLS certificate verification failed: %s", verify_buffer);
        return -1;
    }
    return 0;
}

static int io_write_all(io_context *context, const uint8_t *buffer, size_t length, acidify_http_response *response) {
    size_t written = 0;
    while (written < length) {
        int ret = context->use_tls
            ? mbedtls_ssl_write(&context->ssl, buffer + written, length - written)
            : (int) send(context->net.fd, buffer + written, length - written, 0);
        if (ret == MBEDTLS_ERR_SSL_WANT_READ || ret == MBEDTLS_ERR_SSL_WANT_WRITE) {
            continue;
        }
        if (ret <= 0) {
            set_error(response, "Failed to write request body");
            return -1;
        }
        written += (size_t) ret;
    }
    return 0;
}

static int io_read_all(io_context *context, acidify_http_response *response) {
    size_t capacity = 16384;
    size_t size = 0;
    uint8_t *buffer = (uint8_t *) malloc(capacity);
    if (buffer == NULL) {
        set_error(response, "Out of memory while reading response");
        return -1;
    }

    while (true) {
        if (size == capacity) {
            capacity *= 2;
            uint8_t *new_buffer = (uint8_t *) realloc(buffer, capacity);
            if (new_buffer == NULL) {
                free(buffer);
                set_error(response, "Out of memory while expanding response buffer");
                return -1;
            }
            buffer = new_buffer;
        }

        int ret = context->use_tls
            ? mbedtls_ssl_read(&context->ssl, buffer + size, capacity - size)
            : (int) recv(context->net.fd, buffer + size, capacity - size, 0);
        if (ret == MBEDTLS_ERR_SSL_WANT_READ || ret == MBEDTLS_ERR_SSL_WANT_WRITE) {
            continue;
        }
        if (context->use_tls && ret == MBEDTLS_ERR_SSL_PEER_CLOSE_NOTIFY) {
            break;
        }
        if (ret < 0) {
            free(buffer);
            set_error(response, "Failed to read response body");
            return -1;
        }
        if (ret == 0) {
            break;
        }
        size += (size_t) ret;
    }

    response->raw_response = buffer;
    response->raw_response_len = size;
    return 0;
}

static char *build_request_string(const parsed_url *url, const acidify_http_request *request, size_t *out_size) {
    size_t capacity = 4096 + request->body_len + request->header_count * 256;
    char *buffer = (char *) malloc(capacity);
    if (buffer == NULL) return NULL;

    bool default_port = (url->is_https && strcmp(url->port, "443") == 0) || (!url->is_https && strcmp(url->port, "80") == 0);
    int offset = snprintf(
        buffer,
        capacity,
        "%s %s HTTP/1.1\r\nHost: %s%s%s\r\nConnection: close\r\nAccept-Encoding: identity\r\n",
        request->method,
        url->path,
        url->host,
        default_port ? "" : ":",
        default_port ? "" : url->port
    );
    if (offset < 0 || (size_t) offset >= capacity) {
        free(buffer);
        return NULL;
    }

    for (size_t i = 0; i < request->header_count; ++i) {
        const acidify_http_header *header = &request->headers[i];
        int written = snprintf(buffer + offset, capacity - (size_t) offset, "%s: %s\r\n", header->name, header->value);
        if (written < 0 || (size_t) written >= capacity - (size_t) offset) {
            free(buffer);
            return NULL;
        }
        offset += written;
    }
    if (request->content_type != NULL && request->content_type[0] != '\0') {
        int written = snprintf(buffer + offset, capacity - (size_t) offset, "Content-Type: %s\r\n", request->content_type);
        if (written < 0 || (size_t) written >= capacity - (size_t) offset) {
            free(buffer);
            return NULL;
        }
        offset += written;
    }
    if (request->body != NULL && request->body_len > 0) {
        int written = snprintf(buffer + offset, capacity - (size_t) offset, "Content-Length: %zu\r\n", request->body_len);
        if (written < 0 || (size_t) written >= capacity - (size_t) offset) {
            free(buffer);
            return NULL;
        }
        offset += written;
    }
    if (offset + 4 + request->body_len > (int) capacity) {
        free(buffer);
        return NULL;
    }
    memcpy(buffer + offset, "\r\n", 2);
    offset += 2;
    if (request->body != NULL && request->body_len > 0) {
        memcpy(buffer + offset, request->body, request->body_len);
        offset += (int) request->body_len;
    }
    *out_size = (size_t) offset;
    return buffer;
}

int acidify_http_execute(const acidify_http_request *request, acidify_http_response *response) {
    memset(response, 0, sizeof(*response));
    if (request == NULL || request->method == NULL || request->url == NULL) {
        set_error(response, "HTTP request is incomplete");
        return -1;
    }

    parsed_url url;
    if (parse_url(request->url, &url) != 0) {
        set_error(response, "Unsupported or invalid URL: %s", request->url);
        return -1;
    }

    size_t request_size = 0;
    char *request_buffer = build_request_string(&url, request, &request_size);
    if (request_buffer == NULL) {
        free_parsed_url(&url);
        set_error(response, "Failed to build HTTP request");
        return -1;
    }

    io_context context;
    io_context_init(&context);
    int result = 0;
    if (connect_io(&url, request, &context, response) != 0) {
        result = -1;
        goto cleanup;
    }
    if (io_write_all(&context, (const uint8_t *) request_buffer, request_size, response) != 0) {
        result = -1;
        goto cleanup;
    }
    if (io_read_all(&context, response) != 0) {
        result = -1;
        goto cleanup;
    }

cleanup:
    io_context_free(&context);
    free(request_buffer);
    free_parsed_url(&url);
    return result;
}

void acidify_http_response_free(acidify_http_response *response) {
    if (response == NULL) return;
    free(response->raw_response);
    free(response->error_message);
    response->raw_response = NULL;
    response->raw_response_len = 0;
    response->error_message = NULL;
}
