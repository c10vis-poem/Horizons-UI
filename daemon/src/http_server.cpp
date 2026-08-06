#include "http_server.h"
#include <iostream>
#include <sstream>
#include <cstring>
#include <unistd.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>

// Minimal single-threaded HTTP server for localhost-only daemon IPC.
// No TLS, no keep-alive, no chunked encoding — just enough for NpuClient.kt.
// Production: swap for httplib.h or embed mongoose if concurrency is needed.

struct HttpServer::Impl {
    int port;
    int server_fd = -1;
    bool running = false;
    GenerateHandler gen_handler;
    HealthHandler health_handler;

    void handle_connection(int client_fd) {
        // KNOWN LIMITATION: single recv() into an 8KB buffer. Fine for text-only
        // /api/v1/generate bodies, but the "image_b64" field NpuClient.kt now sends
        // for vision (session 16) is typically 100KB+ and WILL be truncated here.
        // Needs a real read-until-Content-Length loop before vision requests can
        // actually round-trip; not done in this pass (contract-only scaffolding —
        // see engine.h's GenerateRequest::image_b64 doc).
        char buf[8192];
        int n = recv(client_fd, buf, sizeof(buf) - 1, 0);
        if (n <= 0) { close(client_fd); return; }
        buf[n] = '\0';

        std::string raw(buf, n);
        auto first_line_end = raw.find("\r\n");
        std::string first_line = raw.substr(0, first_line_end);

        std::string method, path;
        {
            std::istringstream iss(first_line);
            iss >> method >> path;
        }

        // Extract body (after \r\n\r\n)
        std::string body;
        auto body_start = raw.find("\r\n\r\n");
        if (body_start != std::string::npos) {
            body = raw.substr(body_start + 4);
        }

        if (method == "GET" && path == "/health") {
            bool ok = health_handler();
            std::string resp = ok
                ? "HTTP/1.1 200 OK\r\nContent-Length: 2\r\n\r\nok"
                : "HTTP/1.1 503 Service Unavailable\r\nContent-Length: 9\r\n\r\nnot ready";
            send(client_fd, resp.c_str(), resp.size(), 0);
        } else if (method == "POST" && path == "/api/v1/generate") {
            // SSE response
            std::string header =
                "HTTP/1.1 200 OK\r\n"
                "Content-Type: text/event-stream\r\n"
                "Cache-Control: no-cache\r\n"
                "Connection: close\r\n"
                "\r\n";
            send(client_fd, header.c_str(), header.size(), 0);

            gen_handler(body, [client_fd](const std::string& token, int index, bool done) -> bool {
                std::ostringstream sse;
                sse << "data: {\"token\":\"";
                // Escape JSON special chars
                for (char c : token) {
                    switch (c) {
                        case '"': sse << "\\\""; break;
                        case '\\': sse << "\\\\"; break;
                        case '\n': sse << "\\n"; break;
                        case '\r': sse << "\\r"; break;
                        case '\t': sse << "\\t"; break;
                        default: sse << c;
                    }
                }
                sse << "\",\"index\":" << index
                    << ",\"done\":" << (done ? "true" : "false") << "}\n\n";
                std::string chunk = sse.str();
                int sent = send(client_fd, chunk.c_str(), chunk.size(), MSG_NOSIGNAL);
                return sent > 0;
            });
        } else {
            std::string resp = "HTTP/1.1 404 Not Found\r\nContent-Length: 9\r\n\r\nnot found";
            send(client_fd, resp.c_str(), resp.size(), 0);
        }

        close(client_fd);
    }
};

HttpServer::HttpServer(int port, GenerateHandler gen, HealthHandler health)
    : impl_(std::make_unique<Impl>()) {
    impl_->port = port;
    impl_->gen_handler = std::move(gen);
    impl_->health_handler = std::move(health);
}

HttpServer::~HttpServer() { stop(); }

void HttpServer::run() {
    auto& d = *impl_;
    d.server_fd = socket(AF_INET, SOCK_STREAM, 0);
    if (d.server_fd < 0) {
        std::cerr << "[http] socket() failed\n";
        return;
    }

    int opt = 1;
    setsockopt(d.server_fd, SOL_SOCKET, SO_REUSEADDR, &opt, sizeof(opt));

    sockaddr_in addr{};
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK); // 127.0.0.1 only
    addr.sin_port = htons(d.port);

    if (bind(d.server_fd, (sockaddr*)&addr, sizeof(addr)) < 0) {
        std::cerr << "[http] bind() failed on port " << d.port << "\n";
        close(d.server_fd);
        return;
    }

    listen(d.server_fd, 4);
    d.running = true;
    std::cerr << "[http] listening on 127.0.0.1:" << d.port << "\n";

    while (d.running) {
        sockaddr_in client_addr{};
        socklen_t client_len = sizeof(client_addr);
        int client_fd = accept(d.server_fd, (sockaddr*)&client_addr, &client_len);
        if (client_fd < 0) continue;
        d.handle_connection(client_fd);
    }
}

void HttpServer::stop() {
    if (impl_ && impl_->running) {
        impl_->running = false;
        if (impl_->server_fd >= 0) {
            close(impl_->server_fd);
            impl_->server_fd = -1;
        }
    }
}
