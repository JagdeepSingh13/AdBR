package com.adbr.contentservice.model;

//flow -> pending, uploaded, encoding, encoded, ready / failed

public enum VideoStatus {

    PENDING,   // added but not uploaded to s3
    UPLOADED,
    ENCODING,
    ENCODED,
    READY,   // HLS playlist ready to be streamed
    FAILED

}
