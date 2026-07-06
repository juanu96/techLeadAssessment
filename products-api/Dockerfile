FROM golang:1.21-alpine AS build

WORKDIR /src

COPY go.mod ./
COPY cmd ./cmd
COPY internal ./internal

RUN test -z "$(gofmt -l .)"
RUN go vet ./...
RUN go test ./...
RUN CGO_ENABLED=0 GOOS=linux go build -trimpath -ldflags="-s -w" -o /out/products-api ./cmd/api

FROM alpine:3.20

RUN addgroup -S app && adduser -S -G app app

COPY --from=build /out/products-api /usr/local/bin/products-api

USER app
EXPOSE 8081

ENTRYPOINT ["products-api"]
