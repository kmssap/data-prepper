/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.sink.opensearch.s3;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.http.AbortableInputStream;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectAttributesRequest;
import software.amazon.awssdk.services.s3.model.GetObjectAttributesResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class S3FileReaderTest {

    @Mock
    private S3Client s3Client;

    private S3FileReader s3FileReader;

    @Test
    void readFile_with_validPath() {
        final String fileContent = UUID.randomUUID().toString();
        final String bucketName = UUID.randomUUID().toString();
        final String filePath = UUID.randomUUID().toString().concat(".json");

        final String s3SFile = String.format("s3://%s/%s",bucketName, filePath);

        final InputStream fileObjectStream = IOUtils.toInputStream(fileContent, StandardCharsets.UTF_8);
        final ResponseInputStream<GetObjectResponse> fileInputStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                AbortableInputStream.create(fileObjectStream)
        );

        final GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(bucketName).key(filePath).build();
        final GetObjectAttributesResponse getObjectAttributesResponse = GetObjectAttributesResponse.builder().objectSize(100L).build();

        when(s3Client.getObject(getObjectRequest)).thenReturn(fileInputStream);
        when(s3Client.getObjectAttributes(any(GetObjectAttributesRequest.class))).thenReturn(getObjectAttributesResponse);

        s3FileReader = new S3FileReader(s3Client);

        final ResponseInputStream<GetObjectResponse> file = s3FileReader.readFile(s3SFile);

        assertThat(file, is(fileInputStream));
    }

    @Test
    void readFile_with_invalidPath_throws_parsing_exception() {
        final String bucketName = UUID.randomUUID().toString();
        final String filePath = UUID.randomUUID().toString().concat(".json");

        final String s3File = String.format("s3://%s/%s",bucketName, filePath);
        final GetObjectAttributesResponse getObjectAttributesResponse = GetObjectAttributesResponse.builder().objectSize(100L).build();

        s3FileReader = new S3FileReader(s3Client);

        when(s3Client.getObject(Mockito.any(GetObjectRequest.class))).thenThrow(new RuntimeException("S3 exception"));
        when(s3Client.getObjectAttributes(any(GetObjectAttributesRequest.class))).thenReturn(getObjectAttributesResponse);

        assertThrows(RuntimeException.class, () -> s3FileReader.readFile(s3File));
    }

    @Test
    void readFile_with_bigger_object_throws_S3ObjectTooLargeException() {
        final String bucketName = UUID.randomUUID().toString();
        final String filePath = UUID.randomUUID().toString().concat(".json");

        final String s3SFile = String.format("s3://%s/%s",bucketName, filePath);

        final GetObjectAttributesResponse getObjectAttributesResponse = GetObjectAttributesResponse.builder().objectSize(8_000_000L).build();

        when(s3Client.getObjectAttributes(any(GetObjectAttributesRequest.class))).thenReturn(getObjectAttributesResponse);

        s3FileReader = new S3FileReader(s3Client);

        assertThrows(S3ObjectTooLargeException.class, () -> s3FileReader.readFile(s3SFile));
    }

    @Test
    void readFile_with_unsupported_file_type() {
        final String bucketName = UUID.randomUUID().toString();
        final String filePath = UUID.randomUUID().toString();

        final String s3SFile = String.format("s3://%s/%s",bucketName, filePath);

        s3FileReader = new S3FileReader(s3Client);

        assertThrows(UnsupportedFileTypeException.class, () -> s3FileReader.readFile(s3SFile));
    }
}