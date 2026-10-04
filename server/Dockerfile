# PuzzleIt backend: a PocketBase (https://pocketbase.io) with the collections of pb_migrations. Builds for amd64 and arm64 (Raspberry Pi 4/5, 64-bit OS).
FROM alpine:3.20
ARG PB_VERSION=0.40.5
ARG TARGETARCH
# SHA-256 of the release zips (from the release's checksums.txt): bump them together with PB_VERSION.
RUN case "$TARGETARCH" in \
      amd64) sha=1dbea1b01eb3dfe64953513859d01e759c96650c4e3264fa4eecd54b769382e4 ;; \
      arm64) sha=570baa1c45d2fb92fc0073582b945ca0ece03cbf9837ccabf49a222f99dd02e3 ;; \
      *) echo "unsupported arch $TARGETARCH"; exit 1 ;; \
    esac \
 && apk add --no-cache ca-certificates unzip \
 && wget -q "https://github.com/pocketbase/pocketbase/releases/download/v${PB_VERSION}/pocketbase_${PB_VERSION}_linux_${TARGETARCH}.zip" -O /tmp/pb.zip \
 && echo "$sha  /tmp/pb.zip" | sha256sum -c - \
 && unzip -q /tmp/pb.zip -d /pb && rm /tmp/pb.zip
COPY pb_migrations /pb/pb_migrations
COPY pb_hooks /pb/pb_hooks
EXPOSE 8090
VOLUME /pb/pb_data
CMD ["/pb/pocketbase", "serve", "--http=0.0.0.0:8090", "--dir=/pb/pb_data", "--migrationsDir=/pb/pb_migrations", "--hooksDir=/pb/pb_hooks"]
