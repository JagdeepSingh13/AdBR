## Adaptive Bitrate Streaming (HLD Architecture)
![Project HLD Architecture](https://github.com/JagdeepSingh13/AdBR/blob/main/images/Screenshot%202026-05-11%20174649.png?raw=true)


## Encoding Directory Structure

```text
/tmp/encoding/
└── abc123/
    ├── raw_video.mp4
    │
    └── encoded/
        ├── master.m3u8
        │
        ├── 1080p/
        │   ├── playlist.m3u8
        │   ├── segment0.ts
        │   ├── segment1.ts
        │   ├── segment2.ts
        │   └── ...
        │
        ├── 720p/
        │   ├── playlist.m3u8
        │   ├── segment0.ts
        │   ├── segment1.ts
        │   └── ...
        │
        ├── 480p/
        │   ├── playlist.m3u8
        │   ├── segment0.ts
        │   └── ...
        │
        └── 360p/
            ├── playlist.m3u8
            ├── segment0.ts
            └── ...
```

## S3 Structure

```text
s3://bucket-name/
└── encoded/
    └── abc123/
        ├── master.m3u8
        ├── 1080p/
        ├── 720p/
        ├── 480p/
        └── 360p/
```